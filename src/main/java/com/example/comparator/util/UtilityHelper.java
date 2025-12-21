<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Ultra-Resilient Java-to-JSON</title>
    <style>
        body { font-family: 'Segoe UI', system-ui, sans-serif; padding: 20px; background: #f0f2f5; color: #333; }
        .card { background: white; padding: 25px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); max-width: 1100px; margin: auto; }
        .input-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
        textarea { width: 100%; height: 140px; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-family: 'Consolas', monospace; box-sizing: border-box; font-size: 13px; background: #fafafa; }
        .toolbar { display: flex; gap: 15px; align-items: center; margin: 20px 0; flex-wrap: wrap; background: #f8f9fa; padding: 15px; border-radius: 8px; border: 1px solid #eee; }
        .search-box { flex-grow: 1; padding: 10px 15px; border: 1px solid #ccc; border-radius: 6px; }
        #field-selector { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; margin: 15px 0; max-height: 250px; overflow-y: auto; padding: 15px; border: 1px solid #eee; background: #fff; border-radius: 8px; }
        .field-item { font-size: 13px; display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 6px; border: 1px solid #f0f0f0; cursor: pointer; }
        .field-item:hover { background: #e9ecef; }
        .profile-section { display: flex; gap: 8px; align-items: center; border-left: 2px solid #ddd; padding-left: 15px; }
        pre { background: #1e1e1e; color: #d4d4d4; padding: 20px; border-radius: 8px; overflow-x: auto; font-size: 14px; border: 1px solid #333; }
        button { padding: 9px 18px; border-radius: 6px; border: none; cursor: pointer; font-weight: 600; transition: 0.2s; }
        .btn-primary { background: #007bff; color: white; }
        .btn-success { background: #28a745; color: white; width: 100%; font-size: 16px; padding: 15px; margin-top: 10px; }
        .btn-outline { background: white; border: 1px solid #ddd; color: #555; }
        .btn-copy { background: #6c757d; color: white; }
        .toast { visibility: hidden; min-width: 200px; background-color: #333; color: #fff; text-align: center; border-radius: 4px; padding: 10px; position: fixed; z-index: 1; left: 50%; bottom: 30px; transform: translateX(-50%); }
        .toast.show { visibility: visible; animation: fadein 0.5s, fadeout 0.5s 2.5s; }
        @keyframes fadein { from {bottom: 0; opacity: 0;} to {bottom: 30px; opacity: 1;} }
        @keyframes fadeout { from {bottom: 30px; opacity: 1;} to {bottom: 0; opacity: 0;} }
    </style>
</head>
<body>

<div class="card">
    <div class="input-header">
        <label style="font-weight: 600;">Paste Data (Java toString or JSON):</label>
    </div>
    <textarea id="input" placeholder="Paste Company(name='TechCorp', departments=[...]) here..."></textarea>

    <div class="toolbar">
        <button class="btn-primary" onclick="loadFields()">Analyze Fields</button>
        <label style="font-size: 13px; cursor: pointer;"><input type="checkbox" id="includeNulls"> Include Nulls</label>
        
        <div class="profile-section">
            <input type="text" id="profileName" placeholder="Profile Name" style="padding: 8px; width: 120px; border: 1px solid #ccc; border-radius: 4px;">
            <button class="btn-outline" onclick="saveProfile()">Save Profile</button>
            <select id="profileSelect" onchange="applyProfile()" style="padding: 8px; border-radius: 4px;">
                <option value="">-- Apply Profile --</option>
            </select>
        </div>
    </div>

    <div id="filter-area" style="display:none;">
        <div style="display: flex; gap: 10px; margin-bottom: 10px;">
            <input type="text" id="searchFields" class="search-box" placeholder="Search fields..." oninput="filterDisplay()">
            <button class="btn-outline" onclick="toggleAll(true)">All</button>
            <button class="btn-outline" onclick="toggleAll(false)">None</button>
        </div>
        <div id="field-selector"></div>
        <button class="btn-success" onclick="generateJson()">Generate & Clean JSON</button>
    </div>

    <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 25px;">
        <h3 style="margin:0;">Postman Payload</h3>
        <button class="btn-copy" onclick="copyToClipboard()">Copy JSON</button>
    </div>
    <pre id="output">{}</pre>
</div>

<div id="toast" class="toast">Action Successful!</div>

<script>
    let currentData = {};
    let profiles = JSON.parse(localStorage.getItem('javaJsonProfiles') || '{}');

    // UNIVERSAL PARSER: Handles nesting, lists, and key spaces
    function parseUniversal(str) {
        str = str.trim();
        
        // 1. Detect if it's already a JSON string
        if (str.startsWith('{') || str.startsWith('[')) {
            try { return JSON.parse(str); } catch(e) { /* fall back to custom parser */ }
        }

        // 2. Extract content between outer brackets
        const startIdx = str.search(/[\(\{\[]/);
        const endIdx = Math.max(str.lastIndexOf(')'), str.lastIndexOf('}'), str.lastIndexOf(']'));
        if (startIdx === -1) return formatValue(str);

        const content = str.substring(startIdx + 1, endIdx);
        const result = {};
        let i = 0;

        while (i < content.length) {
            // Find key (anything up to '=' or ':')
            let eqIdx = -1;
            for (let j = i; j < content.length; j++) {
                if (content[j] === '=' || content[j] === ':') { eqIdx = j; break; }
            }
            if (eqIdx === -1) break;

            let key = content.substring(i, eqIdx).trim().replace(/^,/, '').trim();
            i = eqIdx + 1;

            // Find value (handles nesting)
            let valStart = i;
            let bracketStack = [];
            let inQuotes = false;
            let valEnd = content.length;

            for (let j = i; j < content.length; j++) {
                let char = content[j];
                if (char === "'" || char === '"') inQuotes = !inQuotes;
                if (inQuotes) continue;

                if (char === '(' || char === '{' || char === '[') bracketStack.push(char);
                if (char === ')' || char === '}' || char === ']') bracketStack.pop();

                if (bracketStack.length === 0 && char === ',' && !inQuotes) {
                    valEnd = j;
                    break;
                }
            }

            let valStr = content.substring(valStart, valEnd).trim();
            result[key] = processValue(valStr);
            i = valEnd + 1;
        }
        return result;
    }

    function processValue(val) {
        if (val === "null") return null;
        if (val.startsWith('[') && val.endsWith(']')) {
            return splitList(val.slice(1, -1)).map(item => processValue(item.trim()));
        }
        if (val.includes('(') || val.includes('{')) {
            return parseUniversal(val);
        }
        return formatValue(val);
    }

    function splitList(listStr) {
        let results = [];
        let start = 0, bracketStack = 0, inQuotes = false;
        for (let i = 0; i < listStr.length; i++) {
            let c = listStr[i];
            if (c === "'" || c === '"') inQuotes = !inQuotes;
            if (!inQuotes) {
                if (c === '(' || c === '[' || c === '{') bracketStack++;
                if (c === ')' || c === ']' || c === '}') bracketStack--;
                if (c === ',' && bracketStack === 0) {
                    results.push(listStr.substring(start, i));
                    start = i + 1;
                }
            }
        }
        results.push(listStr.substring(start));
        return results;
    }

    function formatValue(v) {
        v = v.trim().replace(/^['"]|['"]$/g, '');
        if (v === "true") return true;
        if (v === "false") return false;
        return (!isNaN(v) && v !== "") ? Number(v) : v;
    }

    // -- UI LOGIC --
    function loadFields() {
        currentData = parseUniversal(document.getElementById('input').value);
        renderFieldList();
        document.getElementById('filter-area').style.display = 'block';
        updateProfileDropdown();
    }

    function renderFieldList() {
        const container = document.getElementById('field-selector');
        container.innerHTML = '';
        Object.keys(currentData).forEach(key => {
            const div = document.createElement('div');
            div.className = 'field-item';
            div.innerHTML = `<input type="checkbox" id="chk_${key}" checked value="${key}"> <label for="chk_${key}">${key}</label>`;
            container.appendChild(div);
        });
    }

    function generateJson() {
        const includeNulls = document.getElementById('includeNulls').checked;
        const selectedKeys = Array.from(document.querySelectorAll('#field-selector input:checked')).map(c => c.value);
        const out = {};
        selectedKeys.forEach(k => {
            const val = currentData[k];
            if (val !== null || includeNulls) out[k] = val;
        });
        document.getElementById('output').innerText = JSON.stringify(out, null, 4);
    }

    function saveProfile() {
        const name = document.getElementById('profileName').value;
        if (!name) return alert("Enter name");
        profiles[name] = Array.from(document.querySelectorAll('#field-selector input:checked')).map(c => c.value);
        localStorage.setItem('javaJsonProfiles', JSON.stringify(profiles));
        updateProfileDropdown();
        showToast("Profile Saved");
    }

    function applyProfile() {
        const name = document.getElementById('profileSelect').value;
        if (!name) return;
        const savedKeys = profiles[name];
        document.querySelectorAll('#field-selector input').forEach(chk => {
            chk.checked = savedKeys.includes(chk.value);
        });
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

    function filterDisplay() {
        const term = document.getElementById('searchFields').value.toLowerCase();
        document.querySelectorAll('.field-item').forEach(item => {
            item.style.display = item.innerText.toLowerCase().includes(term) ? 'flex' : 'none';
        });
    }

    function toggleAll(state) {
        document.querySelectorAll('#field-selector input').forEach(c => c.checked = state);
    }

    function copyToClipboard() {
        navigator.clipboard.writeText(document.getElementById('output').innerText);
        showToast("Copied to Clipboard!");
    }

    function showToast(msg) {
        const x = document.getElementById("toast");
        x.innerText = msg; x.className = "toast show";
        setTimeout(() => x.className = "toast", 3000);
    }

    updateProfileDropdown();
</script>
</body>
</html>
