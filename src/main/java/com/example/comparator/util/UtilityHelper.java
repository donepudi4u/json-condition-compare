<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Spring Architect | Cross-Project Audit</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/js-yaml/4.1.0/js-yaml.min.js"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Inter', sans-serif; }
        .mono { font-family: 'JetBrains Mono', monospace; }
        
        .custom-scrollbar::-webkit-scrollbar { height: 8px; width: 8px; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 10px; }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #cbd5e1; }

        /* Status Styling */
        .row-match { color: #64748b; }
        .row-diff { background-color: #fffbeb; }
        .row-missing { background-color: #fef2f2; }
        
        /* Glassmorphism Sticky Column */
        .sticky-col { 
            position: sticky; left: 0; background: rgba(255, 255, 255, 0.95); 
            backdrop-filter: blur(8px); z-index: 20; 
            border-right: 1px solid #e2e8f0; min-width: 420px;
        }
        
        th { position: sticky; top: 0; background: #f8fafc; z-index: 30; border-bottom: 1px solid #e2e8f0; }

        .value-cell { 
            min-width: 320px; max-width: 600px; word-break: break-all; 
            white-space: pre-wrap; font-size: 13px; line-height: 1.6;
        }

        .gradient-border {
            border: 1px solid transparent;
            background: linear-gradient(white, white) padding-box, 
                        linear-gradient(to right, #6366f1, #a855f7) border-box;
        }
    </style>
</head>
<body class="bg-[#fcfcfd] text-[#1e293b] min-h-screen pb-12">

    <nav class="border-b border-slate-200 bg-white/80 backdrop-blur-md sticky top-0 z-50 px-6 py-4">
        <div class="max-w-[1900px] mx-auto flex justify-between items-center">
            <div class="flex items-center gap-3">
                <div class="w-10 h-10 bg-indigo-600 rounded-xl flex items-center justify-center shadow-lg shadow-indigo-200">
                    <span class="text-white text-xl font-bold">S</span>
                </div>
                <div>
                    <h1 class="text-lg font-extrabold tracking-tight text-slate-900 leading-none">Spring Architect</h1>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">Cross-Project Configuration Audit</p>
                </div>
            </div>
            <button onclick="window.location.reload()" class="text-xs font-bold text-slate-400 hover:text-rose-500 transition-all uppercase tracking-tighter">Reset Session</button>
        </div>
    </nav>

    <div class="max-w-[1900px] mx-auto p-6 space-y-8">
        
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div class="bg-white p-6 rounded-3xl shadow-sm border border-slate-200">
                <div class="flex items-center gap-2 mb-4">
                    <div class="w-2 h-6 bg-blue-500 rounded-full"></div>
                    <input type="text" id="proj1Name" value="Primary-Service" class="bg-transparent font-bold text-slate-700 outline-none focus:text-blue-600 transition-colors">
                </div>
                <label class="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed border-slate-100 rounded-2xl cursor-pointer bg-slate-50/50 hover:bg-blue-50/50 hover:border-blue-200 transition-all group">
                    <span class="text-xs font-bold text-slate-400 group-hover:text-blue-600">Select Project 1 YAML Files</span>
                    <input type="file" id="fileInput1" multiple class="hidden" accept=".yml,.yaml">
                </label>
            </div>

            <div class="bg-white p-6 rounded-3xl shadow-sm border border-slate-200">
                <div class="flex items-center gap-2 mb-4">
                    <div class="w-2 h-6 bg-emerald-500 rounded-full"></div>
                    <input type="text" id="proj2Name" value="Target-Service" class="bg-transparent font-bold text-slate-700 outline-none focus:text-emerald-600 transition-colors">
                </div>
                <label class="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed border-slate-100 rounded-2xl cursor-pointer bg-slate-50/50 hover:bg-emerald-50/50 hover:border-emerald-200 transition-all group">
                    <span class="text-xs font-bold text-slate-400 group-hover:text-emerald-600">Select Project 2 YAML Files</span>
                    <input type="file" id="fileInput2" multiple class="hidden" accept=".yml,.yaml">
                </label>
            </div>
        </div>

        <section id="uiControls" class="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hidden animate-in fade-in duration-500">
            <div class="space-y-6">
                <div>
                    <h3 class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-4">Active Profiles Comparison Matrix</h3>
                    <div id="profilePills" class="flex flex-wrap gap-3"></div>
                </div>

                <div class="flex flex-col md:flex-row gap-6 items-center border-t border-slate-50 pt-6">
                    <div class="flex gap-1 p-1 bg-slate-100 rounded-xl h-fit">
                        <button onclick="setViewMode('all')" id="btn-all" class="view-btn px-6 py-2 rounded-lg text-xs font-bold bg-white text-indigo-600 shadow-sm">All</button>
                        <button onclick="setViewMode('diff')" id="btn-diff" class="view-btn px-6 py-2 rounded-lg text-xs font-bold text-slate-500 hover:text-indigo-600">Mismatches</button>
                        <button onclick="setViewMode('missing')" id="btn-missing" class="view-btn px-6 py-2 rounded-lg text-xs font-bold text-slate-500 hover:text-indigo-600">Missing</button>
                    </div>
                    <div class="relative flex-1">
                        <input type="text" id="searchInput" placeholder="Filter by property key (e.g. spring.datasource)..." 
                               class="w-full px-5 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-sm focus:bg-white focus:ring-4 focus:ring-indigo-50 outline-none transition-all">
                    </div>
                </div>
            </div>
        </section>

        <main id="tableWrapper" class="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden hidden">
            <div class="overflow-x-auto overflow-y-auto max-h-[65vh] custom-scrollbar">
                <table class="w-full border-separate border-spacing-0">
                    <thead><tr id="tableHeader"></tr></thead>
                    <tbody id="tableBody" class="divide-y divide-slate-100"></tbody>
                </table>
            </div>
        </main>
    </div>

    <script>
        let fullData = {}; 
        let selectedColumns = [];
        let viewMode = 'all';
        let searchTerm = '';

        const setupUploader = (id, inputId) => {
            document.getElementById(inputId).addEventListener('change', async function(e) {
                const projName = document.getElementById(id).value || `Project-${inputId.slice(-1)}`;
                const files = Array.from(e.target.files);
                
                for (let file of files) {
                    const text = await file.text();
                    try {
                        const docs = jsyaml.loadAll(text);
                        docs.forEach((doc, i) => {
                            const internalName = doc?.spring?.config?.activate?.['on-profile'] || doc?.spring?.profiles?.active || doc?.spring?.profiles;
                            const fileName = file.name.replace(/application-|\.ya?ml/g, '');
                            const profileName = internalName || (docs.length === 1 ? fileName : `${fileName}-${i+1}`);
                            
                            const colId = `${projName}::${profileName}`;
                            const flattened = flatten(doc || {});
                            
                            fullData[colId] = fullData[colId] ? { ...fullData[colId], ...flattened } : flattened;
                            if (!selectedColumns.includes(colId)) selectedColumns.push(colId);
                        });
                    } catch (err) { console.error(err); }
                }
                document.getElementById('uiControls').classList.remove('hidden');
                document.getElementById('tableWrapper').classList.remove('hidden');
                render();
            });
        };

        setupUploader('proj1Name', 'fileInput1');
        setupUploader('proj2Name', 'fileInput2');

        function flatten(obj, prefix = '') {
            if (!obj) return {};
            return Object.keys(obj).reduce((acc, k) => {
                const pre = prefix.length ? prefix + '.' : '';
                if (obj[k] !== null && typeof obj[k] === 'object' && !Array.isArray(obj[k])) {
                    Object.assign(acc, flatten(obj[k], pre + k));
                } else { acc[pre + k] = obj[k]; }
                return acc;
            }, {});
        }

        function toggleCol(id) {
            selectedColumns = selectedColumns.includes(id) ? selectedColumns.filter(c => c !== id) : [...selectedColumns, id];
            render();
        }

        function setViewMode(mode) {
            viewMode = mode;
            document.querySelectorAll('.view-btn').forEach(btn => {
                btn.classList.remove('bg-white', 'shadow-sm', 'text-indigo-600');
                btn.classList.add('text-slate-500');
            });
            document.getElementById(`btn-${mode}`).classList.add('bg-white', 'shadow-sm', 'text-indigo-600');
            render();
        }

        document.getElementById('searchInput').addEventListener('input', (e) => {
            searchTerm = e.target.value.toLowerCase();
            render();
        });

        function render() {
            const header = document.getElementById('tableHeader');
            const body = document.getElementById('tableBody');
            const pills = document.getElementById('profilePills');
            const allColIds = Object.keys(fullData).sort();

            pills.innerHTML = allColIds.map(id => {
                const [p, prof] = id.split('::');
                const active = selectedColumns.includes(id);
                const color = id.includes(document.getElementById('proj1Name').value) ? 'blue' : 'emerald';
                
                return `<button onclick="toggleCol('${id}')" class="px-4 py-2 rounded-xl text-[11px] font-bold border transition-all ${active ? `bg-${color}-600 text-white border-${color}-600 shadow-lg shadow-${color}-100` : 'bg-white text-slate-400 border-slate-200 hover:border-slate-300'}">
                    <span class="opacity-60 font-medium">${p}</span> <span class="mx-2 opacity-20">/</span> <span>${prof}</span>
                </button>`;
            }).join('');

            header.innerHTML = '<th class="p-6 text-left text-[10px] font-black text-slate-400 uppercase tracking-widest sticky-col top-0">Property Path</th>';
            selectedColumns.forEach(id => {
                const [p, prof] = id.split('::');
                const isP1 = id.includes(document.getElementById('proj1Name').value);
                header.innerHTML += `<th class="p-6 text-center border-l border-slate-50 min-w-[350px] top-0 bg-slate-50/50">
                    <div class="text-[9px] font-extrabold uppercase tracking-widest ${isP1 ? 'text-blue-500' : 'text-emerald-500'} mb-1">${p}</div>
                    <div class="text-xs font-black text-slate-900 uppercase">${prof}</div>
                </th>`;
            });

            body.innerHTML = '';
            if (selectedColumns.length === 0) return;

            const masterKeys = [...new Set(selectedColumns.flatMap(c => Object.keys(fullData[c])))].sort();

            masterKeys.forEach(key => {
                if (searchTerm && !key.toLowerCase().includes(searchTerm)) return;
                const vals = selectedColumns.map(c => fullData[c][key]);
                const isMissing = vals.some(v => v === undefined);
                const allSame = vals.every(v => JSON.stringify(v) === JSON.stringify(vals[0]));
                let status = isMissing ? 'MISSING' : (allSame ? 'MATCH' : 'DIFF');

                if (viewMode === 'diff' && status === 'MATCH') return;
                if (viewMode === 'missing' && status !== 'MISSING') return;

                const tr = document.createElement('tr');
                tr.className = `row-${status.toLowerCase()} transition-colors hover:bg-slate-50/80`;
                let html = `<td class="p-6 sticky-col">
                    <code class="text-[12px] font-bold text-slate-700 mono break-all">${key}</code>
                    <div class="flex gap-2 mt-2">
                        <span class="text-[8px] font-black px-2 py-0.5 rounded-md border ${
                            status === 'MATCH' ? 'bg-slate-50 text-slate-400 border-slate-200' : 
                            status === 'DIFF' ? 'bg-amber-50 text-amber-600 border-amber-200' : 
                            'bg-rose-50 text-rose-600 border-rose-200'
                        }">${status}</span>
                    </div>
                </td>`;

                selectedColumns.forEach(c => {
                    const v = fullData[c][key];
                    html += `<td class="p-6 border-l border-slate-50 align-top">
                        <div class="value-cell mono text-slate-600">${v === undefined ? '<span class="text-rose-300 opacity-40 font-bold tracking-tighter">NULL</span>' : (v === '' ? '<span class="text-slate-300">[empty]</span>' : v)}</div>
                    </td>`;
                });
                tr.innerHTML = html; body.appendChild(tr);
            });
        }
    </script>
</body>
</html>
