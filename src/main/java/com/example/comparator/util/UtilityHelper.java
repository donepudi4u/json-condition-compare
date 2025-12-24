<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Spring Profile Comparator</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/js-yaml/4.1.0/js-yaml.min.js"></script>
    <style>
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
        .row-match { color: #94a3b8; }
        .row-diff { background-color: #fffbeb; }
        .row-missing { background-color: #fef2f2; }
        .sticky-col { position: sticky; left: 0; background: white; z-index: 10; border-right: 2px solid #e2e8f0; }
    </style>
</head>
<body class="bg-slate-50 text-slate-900 min-h-screen p-4 md:p-8">

    <div class="max-w-[1600px] mx-auto">
        <header class="mb-8 flex flex-col md:flex-row justify-between items-end gap-4">
            <div>
                <h1 class="text-3xl font-black text-indigo-900 tracking-tighter uppercase">Spring Config Comparator</h1>                
            </div>
            <div class="flex gap-4">
                <input type="text" id="searchInput" placeholder="Search keys (e.g. datasource)..." 
                       class="px-4 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:ring-2 focus:ring-indigo-500 w-80 shadow-sm">
            </div>
        </header>

        <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
            <aside class="space-y-6">
                <div class="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <h3 class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-4">Load Files</h3>
                    <label class="flex flex-col items-center justify-center w-full h-24 border-2 border-dashed border-indigo-100 rounded-xl cursor-pointer bg-indigo-50/30 hover:bg-indigo-50 transition-all group">
                        <span class="text-xs font-bold text-indigo-600">Drop application.yml files</span>
                        <input type="file" id="fileInput" multiple class="hidden" accept=".yml,.yaml">
                    </label>
                    <button onclick="clearWorkspace()" class="w-full mt-4 text-[10px] font-black text-rose-500 uppercase tracking-widest hover:underline">Reset All</button>
                </div>

                <div class="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <h3 class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-4">Row Filters</h3>
                    <div class="flex flex-col gap-2" id="filterButtons">
                        <button onclick="setViewMode('all')" id="btn-all" class="view-btn bg-indigo-600 text-white px-3 py-2 rounded-lg text-xs font-bold transition-all shadow-md">Show All Properties</button>
                        <button onclick="setViewMode('diff')" id="btn-diff" class="view-btn bg-white text-slate-600 border border-slate-200 px-3 py-2 rounded-lg text-xs font-bold transition-all hover:bg-slate-50">Show Only Diffs</button>
                        <button onclick="setViewMode('missing')" id="btn-missing" class="view-btn bg-white text-slate-600 border border-slate-200 px-3 py-2 rounded-lg text-xs font-bold transition-all hover:bg-slate-50">Show Only Missing</button>
                    </div>
                    
                    <div class="mt-8">
                        <h3 class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3">Compare Profiles</h3>
                        <div id="profileChecklist" class="space-y-1 max-h-60 overflow-y-auto custom-scrollbar">
                            <p class="text-xs text-slate-400 italic">No profiles loaded...</p>
                        </div>
                    </div>
                </div>
            </aside>

            <main class="lg:col-span-3 bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                <div class="overflow-x-auto custom-scrollbar">
                    <table class="w-full border-collapse" id="comparisonTable">
                        <thead class="bg-slate-50 border-b border-slate-200">
                            <tr id="tableHeader">
                                <th class="p-4 text-left text-[10px] font-black text-slate-400 uppercase tracking-widest sticky-col">Property Path</th>
                            </tr>
                        </thead>
                        <tbody id="tableBody" class="divide-y divide-slate-100 text-sm">
                            <tr><td class="p-20 text-center text-slate-400 italic" colspan="100%">Upload YAML files to see the comparison matrix.</td></tr>
                        </tbody>
                    </table>
                </div>
            </main>
        </div>
    </div>

    <script>
        let profilesData = {}; // Store all parsed profiles
        let selectedProfileNames = []; // Active profiles for comparison
        let viewMode = 'all';
        let searchTerm = '';

        function flatten(obj, prefix = '') {
            return Object.keys(obj).reduce((acc, k) => {
                const pre = prefix.length ? prefix + '.' : '';
                if (obj[k] !== null && typeof obj[k] === 'object' && !Array.isArray(obj[k])) {
                    Object.assign(acc, flatten(obj[k], pre + k));
                } else {
                    acc[pre + k] = obj[k];
                }
                return acc;
            }, {});
        }

        document.getElementById('fileInput').addEventListener('change', function(e) {
            const files = Array.from(e.target.files);
            let processedCount = 0;

            files.forEach(file => {
                const reader = new FileReader();
                reader.onload = (event) => {
                    try {
                        const docs = jsyaml.loadAll(event.target.result);
                        docs.forEach((doc, i) => {
                            const internalName = doc?.spring?.config?.activate?.['on-profile'] || doc?.spring?.profiles?.active || doc?.spring?.profiles;
                            const fileName = file.name.replace(/application-|\.ya?ml/g, '');
                            const finalName = internalName || (docs.length === 1 ? fileName : `${fileName}-${i+1}`);
                            
                            profilesData[finalName] = flatten(doc || {});
                            if (!selectedProfileNames.includes(finalName)) {
                                selectedProfileNames.push(finalName);
                            }
                        });
                    } catch (err) { console.error("Parse Error", err); }
                    
                    processedCount++;
                    if (processedCount === files.length) render();
                };
                reader.readAsText(file);
            });
        });

        function setViewMode(mode) {
            viewMode = mode;
            document.querySelectorAll('.view-btn').forEach(btn => {
                btn.classList.remove('bg-indigo-600', 'text-white', 'shadow-md');
                btn.classList.add('bg-white', 'text-slate-600', 'border-slate-200');
            });
            const activeBtn = document.getElementById(`btn-${mode}`);
            activeBtn.classList.remove('bg-white', 'text-slate-600', 'border-slate-200');
            activeBtn.classList.add('bg-indigo-600', 'text-white', 'shadow-md');
            render();
        }

        document.getElementById('searchInput').addEventListener('input', (e) => {
            searchTerm = e.target.value.toLowerCase();
            render();
        });

        function toggleProfile(name) {
            if (selectedProfileNames.includes(name)) {
                selectedProfileNames = selectedProfileNames.filter(p => p !== name);
            } else {
                selectedProfileNames.push(name);
            }
            render();
        }

        function clearWorkspace() {
            profilesData = {};
            selectedProfileNames = [];
            document.getElementById('fileInput').value = '';
            render();
        }

        function render() {
            const allProfileNames = Object.keys(profilesData);
            const header = document.getElementById('tableHeader');
            const body = document.getElementById('tableBody');
            const checklist = document.getElementById('profileChecklist');

            // 1. Update Profile Checklist UI
            checklist.innerHTML = allProfileNames.map(name => `
                <label class="flex items-center gap-2 text-[11px] font-bold text-slate-600 p-2 hover:bg-slate-50 rounded-lg cursor-pointer transition-all border border-transparent hover:border-indigo-100">
                    <input type="checkbox" ${selectedProfileNames.includes(name) ? 'checked' : ''} 
                           onchange="toggleProfile('${name}')"
                           class="rounded border-slate-300 text-indigo-600 focus:ring-0"> 
                    <span class="truncate">${name}</span>
                </label>
            `).join('') || '<p class="text-xs text-slate-400 italic">No profiles loaded...</p>';

            // 2. Update Table Header (Only selected profiles)
            header.innerHTML = '<th class="p-4 text-left text-[10px] font-black text-slate-400 uppercase tracking-widest sticky-col">Property Path</th>';
            selectedProfileNames.forEach(name => {
                const th = document.createElement('th');
                th.className = "p-4 text-[10px] font-black text-indigo-900 uppercase tracking-widest border-l border-slate-100 text-center min-w-[150px]";
                th.innerText = name;
                header.appendChild(th);
            });

            // 3. Update Table Body
            body.innerHTML = '';
            if (selectedProfileNames.length === 0) {
                body.innerHTML = '<tr><td class="p-20 text-center text-slate-400 italic" colspan="100%">Select at least one profile to compare.</td></tr>';
                return;
            }

            // Get unique keys only from selected profiles
            const activeKeys = [...new Set(selectedProfileNames.flatMap(p => Object.keys(profilesData[p])))].sort();

            activeKeys.forEach(key => {
                if (searchTerm && !key.toLowerCase().includes(searchTerm)) return;

                const values = selectedProfileNames.map(p => profilesData[p][key]);
                const isMissing = values.some(v => v === undefined);
                const allSame = values.every(v => JSON.stringify(v) === JSON.stringify(values[0]));

                let rowClass = 'row-match';
                let statusLabel = 'MATCH';
                
                if (isMissing) {
                    rowClass = 'row-missing';
                    statusLabel = 'MISSING';
                } else if (!allSame) {
                    rowClass = 'row-diff';
                    statusLabel = 'DIFF';
                }

                if (viewMode === 'diff' && statusLabel === 'MATCH') return;
                if (viewMode === 'missing' && statusLabel !== 'MISSING') return;

                const row = document.createElement('tr');
                row.className = `${rowClass} group transition-colors`;

                let rowHtml = `
                    <td class="p-3 border-b border-slate-100 sticky-col shadow-[2px_0_5px_-2px_rgba(0,0,0,0.05)]">
                        <div class="flex flex-col gap-1">
                            <code class="text-[11px] font-bold text-slate-700 break-all leading-tight">${key}</code>
                            <span class="text-[8px] font-black w-fit px-1.5 rounded border ${
                                statusLabel === 'MATCH' ? 'text-slate-400 border-slate-200' : 
                                statusLabel === 'DIFF' ? 'text-amber-600 border-amber-200 bg-amber-50' : 
                                'text-rose-600 border-rose-200 bg-rose-50'
                            }">${statusLabel}</span>
                        </div>
                    </td>
                `;

                selectedProfileNames.forEach(p => {
                    const val = profilesData[p][key];
                    const isUndefined = val === undefined;
                    rowHtml += `
                        <td class="p-3 border-b border-slate-100 border-l border-slate-50 text-center font-medium">
                            <span class="${isUndefined ? 'text-rose-300 italic font-bold opacity-50' : 'text-slate-800'}">
                                ${isUndefined ? 'NULL' : (val === '' ? '[empty]' : val)}
                            </span>
                        </td>
                    `;
                });

                row.innerHTML = rowHtml;
                body.appendChild(row);
            });
        }
    </script>
</body>
</html>
