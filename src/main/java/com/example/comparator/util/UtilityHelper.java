<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Java toString Pro Converter</title>
    <style>
        body { font-family: 'Segoe UI', system-ui, sans-serif; padding: 20px; background: #f0f2f5; color: #333; }
        .card { background: white; padding: 25px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); max-width: 1100px; margin: auto; }
        textarea { width: 100%; height: 120px; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-family: 'Consolas', monospace; box-sizing: border-box; font-size: 13px; }
        
        .toolbar { display: flex; gap: 15px; align-items: center; margin: 20px 0; flex-wrap: wrap; background: #f8f9fa; padding: 15px; border-radius: 8px; border: 1px solid #eee; }
        .search-box { flex-grow: 1; padding: 10px 15px; border: 1px solid #ccc; border-radius: 6px; font-size: 14px; }
        
        #field-selector { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; margin: 15px 0; max-height: 300px; overflow-y: auto; padding: 15px; border: 1px solid #eee; background: #fff; border-radius: 8px; }
        .field-item { font-size: 13px; display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 6px; border: 1px solid #f0f0f0; transition: 0.2s; }
        .field-item:hover { background: #e9ecef; border-color: #dee2e6; }
        .field-item input { cursor: pointer; width: 16px; height: 16px; }

        .profile-section { display: flex; gap: 8px; align-items: center; border-left: 2px solid #ddd; padding-left: 15px; }
        
        .output-header { display: flex; justify-content: space-between; align-items: center; margin-top: 25px; }
        pre { background: #1e1e1e; color: #d4d4d4; padding: 20px; border-radius: 8px; overflow-x: auto; font-size: 14px; line-height: 1.5; border: 1px solid #333; }
        
        button { padding: 9px 18px; border-radius: 6px; border: none; cursor: pointer; font-weight: 600; transition: 0.2s; display: inline-flex; align-items: center; justify-content: center; }
        .btn-primary { background: #007bff; color: white; }
        .btn-primary:hover { background: #0056b3; }
        .btn-success { background: #28a745; color: white; }
        .btn-success:hover { background: #218838; }
        .btn-outline { background: white; border: 1px solid #ddd; color: #555; }
        .btn-outline:hover { background: #f8f9fa; border-color: #ccc; }
        .btn-copy { background: #6c757d; color: white; margin-left: 10px; }
        .btn-copy:hover { background: #5a6268; }
        .btn-danger { background: #dc3545; color: white; padding: 6px 10px; font-size: 12px; }

        .toast { visibility: hidden; min-width: 200px; background-color: #333; color: #fff; text-align: center; border-radius: 4px; padding: 10px; position: fixed; z-index: 1; left: 50%; bottom: 30px; transform: translateX(-50%); font-size: 14px; }
        .toast.show { visibility: visible; animation: fadein 0.5s, fadeout 0.5s 2.5s; }
        @keyframes fadein { from {bottom: 0; opacity: 0;} to {bottom: 30px; opacity: 1;} }
        @keyframes fadeout { from {bottom: 30px; opacity: 1;} to {bottom: 0; opacity: 0;} }
    </style>
</head>
<body>

<div class="card">
    <h2 style="margin-top:0;">Java <code>toString()</code> to JSON Pro</h2>
    
    <label style="font-size: 14px; font-weight: 600;">1. Paste Java Object String:</label>
    <textarea id="input" style="margin-top: 8px;" placeholder="User(id=101, name=John Doe, roles=[ADMIN, DEV], details=Details(lastLogin=2023-01-01, active=true), meta=null)"></textarea>

    <div class="toolbar">
        <button class="btn-primary" onclick="loadFields()">Load & Analyze Fields</button>
        <label style="font-size: 14px; cursor: pointer;"><input type="checkbox" id="includeNulls"> Include Nulls</label>
        
        <div class="profile-section">
            <input type="text" id="profileName" placeholder="New Profile Name" style="padding: 8px; width: 140px; border-radius: 4px; border: 1px solid #ccc;">
            <button class="btn-outline" onclick="saveProfile()">Save Settings</button>
            <select id="profileSelect" onchange="applyProfile()" style="padding: 8px; border-radius: 4px;">
                <option value="">-- Apply Profile --</option>
            </select>
            <button class="btn-danger" onclick="deleteProfile()">Delete</button>
        </div>
    </div>

    <div id="filter-area" style="display:none;">
        <div style="display: flex; gap: 10px; margin-bottom: 15px;">
            <input type="text" id="searchFields" class="search-box" placeholder="Search through your 100+ fields..." oninput="filterDisplay()">
            <button class="btn-outline" onclick="toggleAll(true)">Select All</button>
            <button class="btn-outline" onclick="toggleAll(false)">Clear All</button>
        </div>
        <div id="field-selector"></div>
        <button class="btn-success" style="width: 100%; font-size: 16px; padding: 15px;" onclick="generateJson()">Generate & Finalize JSON</button>
    </div>

    <div class="output-header">
        <h3 style="margin:0;">Final JSON for Postman</h3>
        <button class="btn-copy" onclick="copyToClipboard()">Copy to Clipboard</button>
    </div>
    <pre id="output">{}</pre>
</div>

<div id="toast" class="toast">JSON Copied to Clipboard!</div>

<script>
    let currentData = {};
    let allKeys = [];
    let profiles = JSON.parse(localStorage.getItem('javaJsonProfiles') || '{}');

    // Recursive Parser for Nested Objects and Lists
    function parseJavaString(str) {
        if (!str.includes('(')) return autoFormat(str.trim());
        let content = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));
        const result = {};
        let regex = /(\w+)=((?:[^\(,\)]|\([^\)]*\)|\[[^\]]*\])*)/g;
        let match;
        while ((match = regex.exec(content)) !== null) {
            let key = match[1].trim();
            let val = match[2].trim();
            if (val === "null") result[key] = null;
            else if (val.startsWith("[") && val.endsWith("]")) {
                result[key] = val.slice(1, -1).split(',').map(i => {
                    let item = i.trim();
                    return item.includes('(') ? parseJavaString(item) : autoFormat(item);
                });
            } else if (val.includes("(") && val.endsWith(")")) {
                result[key] = parseJavaString(val);
            } else {
                result[key] = autoFormat(val);
            }
        }
        return result;
    }

    function autoFormat(v) {
        if (v === "true") return true; 
        if (v === "false") return false;
        return (!isNaN(v) && v !== "") ? Number(v) : v;
    }

    function loadFields() {
        const input = document.getElementById('input').value;
        if(!input) return alert("Please paste a Java string first.");
        currentData = parseJavaString(input);
        allKeys = Object.keys(currentData);
        renderFieldList();
        document.getElementById('filter-area').style.display = 'block';
        updateProfileDropdown();
    }

    function renderFieldList() {
        const container = document.getElementById('field-selector');
        container.innerHTML = '';
        allKeys.forEach(key => {
            const div = document.createElement('div');
            div.className = 'field-item';
            div.innerHTML = `<input type="checkbox" id="chk_${key}" checked value="${key}"> <label for="chk_${key}">${key}</label>`;
            container.appendChild(div);
        });
    }

    function filterDisplay() {
        const term = document.getElementById('searchFields').value.toLowerCase();
        document.querySelectorAll('.field-item').forEach(item => {
            const name = item.innerText.toLowerCase();
            item.style.display = name.includes(term) ? 'flex' : 'none';
        });
    }

    function toggleAll(state) {
        document.querySelectorAll('#field-selector input').forEach(c => c.checked = state);
    }

    function saveProfile() {
        const name = document.getElementById('profileName').value;
        if (!name) return alert("Please enter a profile name.");
        const selected = Array.from(document.querySelectorAll('#field-selector input:checked')).map(c => c.value);
        profiles[name] = selected;
        localStorage.setItem('javaJsonProfiles', JSON.stringify(profiles));
        updateProfileDropdown();
        showToast(`Profile "${name}" saved!`);
    }

    function updateProfileDropdown() {
        const sel = document.getElementById('profileSelect');
        sel.innerHTML = '<option value="">-- Apply Profile --</option>';
        Object.keys(profiles).forEach(p => {
            const opt = document.createElement('option');
            opt.value = p; opt.innerText = p;
            sel.appendChild(opt);
        });
    }

    function applyProfile() {
        const name = document.getElementById('profileSelect').value;
        if (!name || !profiles[name]) return;
        const savedKeys = profiles[name];
        document.querySelectorAll('#field-selector input').forEach(chk => {
            chk.checked = savedKeys.includes(chk.value);
        });
    }

    function deleteProfile() {
        const name = document.getElementById('profileSelect').value;
        if (name && confirm(`Delete profile "${name}"?`)) {
            delete profiles[name];
            localStorage.setItem('javaJsonProfiles', JSON.stringify(profiles));
            updateProfileDropdown();
        }
    }

    function generateJson() {
        const includeNulls = document.getElementById('includeNulls').checked;
        const selectedKeys = Array.from(document.querySelectorAll('#field-selector input:checked')).map(c => c.value);
        const out = {};
        selectedKeys.forEach(k => {
            if (currentData[k] !== null || includeNulls) out[k] = currentData[k];
        });
        document.getElementById('output').innerText = JSON.stringify(out, null, 4);
    }

    function copyToClipboard() {
        const text = document.getElementById('output').innerText;
        navigator.clipboard.writeText(text).then(() => {
            showToast("JSON Copied!");
        });
    }

    function showToast(msg) {
        const x = document.getElementById("toast");
        x.innerText = msg;
        x.className = "toast show";
        setTimeout(() => { x.className = x.className.replace("show", ""); }, 3000);
    }

    // Initialize dropdown on load
    updateProfileDropdown();
</script>
</body>
</html>
