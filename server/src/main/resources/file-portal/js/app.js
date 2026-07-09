(() => {
    'use strict';

    const ICON_BASE = '/icon/';
    const ICON_FILES = {
        folder: 'folder.svg',
        file: 'file.svg',
        image: 'image.svg',
        video: 'video.svg',
        audio: 'audio.svg',
        word: 'word.svg',
        pdf: 'pdf.svg',
        ppt: 'powerpoint.svg',
        excel: 'excel.svg',
        csv: 'csv.svg',
        archive: 'archive.svg',
        html: 'html.svg',
        css: 'css.svg',
        java: 'java.svg',
        markdown: 'markdown.svg',
        xml: 'xml.svg',
        yaml: 'yaml.svg',
        photoshop: 'photoshop.svg',
        apk: 'apk.svg',
        exe: 'exe.svg',
        dmg: 'dmg.svg'
    };

    const EXT_ICON_TYPES = {
        jpg: 'image', jpeg: 'image', png: 'image', gif: 'image', bmp: 'image',
        webp: 'image', svg: 'image', ico: 'image', tiff: 'image', tif: 'image',
        heic: 'image', heif: 'image', raw: 'image', psd: 'photoshop',
        mp4: 'video', avi: 'video', mkv: 'video', mov: 'video', wmv: 'video',
        flv: 'video', webm: 'video', m4v: 'video', mpg: 'video', mpeg: 'video',
        ts: 'video', m2ts: 'video', mts: 'video', '3gp': 'video',
        mp3: 'audio', wav: 'audio', flac: 'audio', aac: 'audio', ogg: 'audio',
        m4a: 'audio', wma: 'audio', aiff: 'audio', ape: 'audio',
        doc: 'word', docx: 'word', rtf: 'word', odt: 'word',
        pdf: 'pdf',
        ppt: 'ppt', pptx: 'ppt', pps: 'ppt', ppsx: 'ppt',
        xls: 'excel', xlsx: 'excel', xlsm: 'excel', xlsb: 'excel',
        csv: 'csv',
        zip: 'archive', rar: 'archive', '7z': 'archive', tar: 'archive',
        gz: 'archive', bz2: 'archive', xz: 'archive', tgz: 'archive', tbz: 'archive',
        jar: 'java', java: 'java', class: 'java',
        html: 'html', htm: 'html', xhtml: 'html',
        css: 'css', scss: 'css', sass: 'css', less: 'css',
        md: 'markdown', markdown: 'markdown',
        xml: 'xml', xsd: 'xml', xsl: 'xml', xslt: 'xml',
        yaml: 'yaml', yml: 'yaml', json: 'yaml',
        js: 'java', mjs: 'java', cjs: 'java', jsx: 'java',
        ts: 'java', tsx: 'java', es: 'java',
        vue: 'html', svelte: 'html',
        apk: 'apk', exe: 'exe', msi: 'exe', bat: 'exe', cmd: 'exe',
        dmg: 'dmg', pkg: 'dmg', deb: 'dmg', rpm: 'dmg', iso: 'dmg'
    };

    const THEME_KEY = 'orbien-file-theme';
    const AUTH_REFRESH_INTERVAL = 30_000;
    const WRITE_CONTROL_IDS = ['btnUpload', 'btnMkdir', 'btnDelete', 'fileInput', 'emptyUploadLink', 'emptyMkdirLink'];
    const THEME_BUTTON_IDS = ['btnTheme', 'btnThemeLogin'];

    let currentPath = '/';
    let entries = [];
    let authRequired = true;
    let canWrite = true;
    let permission = 'read_write';
    let currentUsername = '';
    let authRefreshTimer = null;

    const $ = (id) => document.getElementById(id);

    const entryName = (entry) => entry.name || entry.name_ || '';

    const joinPath = (base, name) => (base === '/' ? `/${name}` : `${base}/${name}`);

    const iconUrl = (type) => ICON_BASE + encodeURIComponent(ICON_FILES[type] || ICON_FILES.file);

    const fileExtension = (name) => {
        const dot = name.lastIndexOf('.');
        if (dot <= 0 || dot === name.length - 1) {
            return '';
        }
        return name.slice(dot + 1).toLowerCase();
    };

    const resolveIconType = (name, isDirectory) => {
        if (isDirectory) {
            return 'folder';
        }
        const ext = fileExtension(name);
        return ext ? (EXT_ICON_TYPES[ext] || 'file') : 'file';
    };

    const createFileIcon = (name, isDirectory) => {
        const img = document.createElement('img');
        img.className = 'file-icon';
        img.alt = '';
        img.src = iconUrl(resolveIconType(name, isDirectory));
        img.onerror = () => {
            img.onerror = null;
            img.src = iconUrl('file');
        };
        return img;
    };

    const fileTypeLabel = (name, isDirectory) => {
        if (isDirectory) {
            return '文件夹';
        }
        const ext = fileExtension(name);
        return ext ? ext.toUpperCase() : '文件';
    };

    const escapeHtml = (text) => String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');

    const formatSize = (n) => {
        if (!n) return '';
        if (n < 1024) return `${n} B`;
        if (n < 1_048_576) return `${(n / 1024).toFixed(1)} KB`;
        return `${(n / 1_048_576).toFixed(1)} MB`;
    };

    const formatTime = (ms) => (ms ? new Date(Number(ms)).toLocaleString('zh-CN') : '');

    const permissionLabel = (value) => (value === 'read_write' ? '读写' : '只读');

    const extractErrorMessage = (data, fallback) => {
        if (!data) return fallback;
        if (data.message) return data.message;
        if (data.status?.message) return data.status.message;
        return fallback;
    };

    const showError = (err) => {
        alert(err.message || String(err));
    };

    const applyTheme = (theme) => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem(THEME_KEY, theme);
        const label = theme === 'dark' ? '白昼' : '黑夜';
        const title = theme === 'dark' ? '切换到白昼模式' : '切换到黑夜模式';
        for (const id of THEME_BUTTON_IDS) {
            const btn = $(id);
            if (btn) {
                btn.textContent = label;
                btn.title = title;
            }
        }
    };

    const initTheme = () => {
        const saved = localStorage.getItem(THEME_KEY);
        applyTheme(saved === 'dark' ? 'dark' : 'light');
    };

    const toggleTheme = () => {
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        applyTheme(current === 'dark' ? 'light' : 'dark');
    };

    const updatePermissionBadge = () => {
        const badge = $('permissionBadge');
        const readonlyTip = $('readonlyTip');
        if (!authRequired) {
            badge.classList.add('hidden');
            readonlyTip.classList.add('hidden');
            return;
        }
        badge.classList.remove('hidden');
        badge.textContent = permissionLabel(permission);
        badge.className = `permission-badge ${canWrite ? 'write' : 'read'}`;
        readonlyTip.classList.toggle('hidden', canWrite);
    };

    const updateWriteControls = () => {
        for (const id of WRITE_CONTROL_IDS) {
            const el = $(id);
            if (el) {
                el.style.display = canWrite ? '' : 'none';
            }
        }
    };

    const showLogin = (message) => {
        $('loginView').classList.remove('hidden');
        $('appView').classList.add('hidden');
        const errEl = $('loginError');
        if (message) {
            errEl.textContent = message;
            errEl.style.display = 'block';
        } else {
            errEl.style.display = 'none';
        }
    };

    const showApp = (username) => {
        $('loginView').classList.add('hidden');
        $('appView').classList.remove('hidden');
        $('currentUser').textContent = username ? `${username} ` : '';
        $('btnLogout').classList.toggle('hidden', !authRequired);
        updatePermissionBadge();
        updateWriteControls();
    };

    async function api(path, options = {}) {
        const res = await fetch(path, {credentials: 'include', ...options});
        const ct = res.headers.get('content-type') || '';
        if (ct.includes('application/json')) {
            const data = await res.json();
            if (res.status === 401) {
                if (authRequired) {
                    showLogin(extractErrorMessage(data, '未登录'));
                }
                throw new Error(extractErrorMessage(data, '未登录'));
            }
            if (res.status === 403) {
                refreshAuthStatus().catch(() => { /* ignore */
                });
                throw new Error(extractErrorMessage(data, '没有操作权限'));
            }
            if (!res.ok) {
                throw new Error(extractErrorMessage(data, `请求失败: ${res.status}`));
            }
            if (data.status && data.status.code !== 0) {
                throw new Error(extractErrorMessage(data, '操作失败'));
            }
            return data;
        }
        if (!res.ok) {
            throw new Error(`请求失败: ${res.status}`);
        }
        return res;
    }

    async function refreshAuthStatus() {
        const res = await fetch('/api/auth/status', {credentials: 'include'});
        const data = await res.json();
        authRequired = !!data.authRequired;
        permission = data.permission || (data.canWrite ? 'read_write' : 'read');
        canWrite = data.canWrite === true;
        currentUsername = data.username || '';

        if (!authRequired) {
            showApp(currentUsername || '访客');
            return data;
        }
        if (data.loggedIn) {
            showApp(currentUsername);
            return data;
        }
        showLogin(data.message || '');
        return data;
    }

    const scheduleAuthRefresh = () => {
        if (authRefreshTimer) {
            clearInterval(authRefreshTimer);
        }
        authRefreshTimer = setInterval(() => {
            refreshAuthStatus().catch(() => { /* ignore background refresh errors */
            });
        }, AUTH_REFRESH_INTERVAL);
    };

    async function login() {
        const username = $('username').value.trim();
        const password = $('password').value;
        const errEl = $('loginError');
        errEl.style.display = 'none';
        try {
            const data = await api('/api/auth/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({username, password})
            });
            canWrite = data.canWrite === true;
            permission = data.permission || (canWrite ? 'read_write' : 'read');
            currentUsername = username;
            showApp(username);
            await loadList();
        } catch (e) {
            errEl.textContent = e.message || '登录失败';
            errEl.style.display = 'block';
        }
    }

    async function logout() {
        try {
            await api('/api/auth/logout', {method: 'POST'});
        } catch { /* ignore */
        }
        canWrite = false;
        permission = 'read';
        showLogin();
    }

    async function ensureWritable(actionName) {
        await refreshAuthStatus();
        if (!canWrite) {
            throw new Error(`当前为只读权限，无法${actionName || '执行此操作'}`);
        }
    }

    async function loadList() {
        try {
            const data = await api(`/api/files/list?path=${encodeURIComponent(currentPath)}`);
            entries = data.entries || data.entriesList || [];
            renderList();
        } catch (e) {
            if (e.message?.includes('目录不存在') || e.message?.includes('不是目录')) {
                if (currentPath !== '/') {
                    const parts = currentPath.split('/').filter(Boolean);
                    parts.pop();
                    currentPath = parts.length ? `/${parts.join('/')}` : '/';
                    return loadList();
                }
            }
            throw e;
        }
    }

    const renderBreadcrumb = () => {
        const parts = currentPath === '/' ? [] : currentPath.split('/').filter(Boolean);
        let html = '<span data-path="/">全部文件</span>';
        let acc = '';
        for (const part of parts) {
            acc += `/${part}`;
            html += ` / <span data-path="${acc}">${escapeHtml(part)}</span>`;
        }
        $('breadcrumb').innerHTML = html;
    };

    const renderList = () => {
        const tbody = $('fileList');
        tbody.innerHTML = '';
        const dirs = entries.filter((e) => e.directory).length;
        const files = entries.length - dirs;
        $('summary').textContent = `全部文件 (${dirs}个目录, ${files}个文件)`;
        $('emptyState').classList.toggle('hidden', entries.length > 0);
        renderBreadcrumb();

        entries.forEach((entry, index) => {
            const name = entryName(entry);
            const tr = document.createElement('tr');

            const tdCheck = document.createElement('td');
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.dataset.idx = String(index);
            tdCheck.appendChild(checkbox);

            const tdName = document.createElement('td');
            const nameSpan = document.createElement('span');
            nameSpan.className = 'name-link';
            nameSpan.appendChild(createFileIcon(name, entry.directory));
            const nameText = document.createElement('span');
            nameText.textContent = name;
            nameSpan.appendChild(nameText);
            if (entry.directory) {
                nameSpan.addEventListener('click', () => enterDir(name));
            }
            tdName.appendChild(nameSpan);

            const tdTime = document.createElement('td');
            tdTime.textContent = formatTime(entry.modifiedTime || entry.modified_time);

            const tdType = document.createElement('td');
            tdType.textContent = fileTypeLabel(name, entry.directory);

            const tdSize = document.createElement('td');
            tdSize.textContent = entry.directory ? '' : formatSize(entry.size);

            tr.append(tdCheck, tdName, tdTime, tdType, tdSize);
            tbody.appendChild(tr);
        });
    };

    const navigate = (e) => {
        if (e.target.dataset.path) {
            currentPath = e.target.dataset.path;
            loadList().catch(showError);
        }
    };

    const enterDir = (name) => {
        currentPath = joinPath(currentPath, name);
        loadList().catch(showError);
    };

    const selectedPaths = () => [...document.querySelectorAll('#fileList input[type=checkbox]:checked')]
        .map((checkbox) => {
            const entry = entries[Number(checkbox.dataset.idx)];
            return joinPath(currentPath, entryName(entry));
        });

    const uploadSingleFile = (file, itemEl) => new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', `/api/files/upload?path=${encodeURIComponent(currentPath)}`);
        xhr.withCredentials = true;
        xhr.upload.onprogress = (ev) => {
            if (ev.lengthComputable) {
                itemEl.querySelector('.bar-fill').style.width = `${(ev.loaded / ev.total) * 100}%`;
            }
        };
        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                itemEl.querySelector('.bar-fill').style.width = '100%';
                resolve();
                return;
            }
            let msg = '上传失败';
            try {
                const body = JSON.parse(xhr.responseText || '{}');
                msg = extractErrorMessage(body, msg);
            } catch { /* ignore */
            }
            reject(new Error(msg));
        };
        xhr.onerror = () => reject(new Error('网络错误'));
        xhr.onabort = () => reject(new Error('上传已取消'));
        const fd = new FormData();
        fd.append('file', file);
        xhr.send(fd);
    });

    async function uploadFiles(files) {
        if (!files.length) return;
        await ensureWritable('上传文件');
        $('progressPanel').style.display = 'block';
        $('uploadCount').textContent = String(files.length);
        const list = $('progressList');
        list.innerHTML = '';
        let hasError = false;

        for (const file of files) {
            const div = document.createElement('div');
            div.className = 'progress-item';
            div.innerHTML = `${escapeHtml(file.name)}<div class="bar"><div class="bar-fill"></div></div>`;
            list.appendChild(div);

            try {
                await uploadSingleFile(file, div);
            } catch (e) {
                hasError = true;
                div.classList.add('error');
                div.querySelector('.bar').remove();
                div.append(document.createTextNode(` — ${e.message || '上传失败'}`));
            }
        }

        try {
            await loadList();
        } catch (e) {
            if (!hasError) {
                throw e;
            }
        }
        $('fileInput').value = '';
        if (hasError) {
            alert('部分文件上传失败，请查看上传面板中的错误信息');
        }
    }

    const parseFilenameFromDisposition = (header) => {
        if (!header) return null;
        const star = /filename\*=([^']*)''([^;]+)/i.exec(header);
        if (star) {
            try {
                return decodeURIComponent(star[2].trim());
            } catch { /* ignore */
            }
        }
        const quoted = /filename="([^"]*)"/i.exec(header);
        if (quoted?.[1]) return quoted[1];
        const plain = /filename=([^;]+)/i.exec(header);
        if (plain?.[1]) return plain[1].trim();
        return null;
    };

    async function downloadFile(path) {
        const res = await fetch(`/api/files/download?path=${encodeURIComponent(path)}`, {credentials: 'include'});
        if (!res.ok) {
            let msg = '下载失败';
            try {
                msg = extractErrorMessage(await res.json(), msg);
            } catch { /* ignore */
            }
            throw new Error(msg);
        }
        const blob = await res.blob();
        const name = parseFilenameFromDisposition(res.headers.get('Content-Disposition'))
            || path.split('/').pop()
            || 'download';
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = name;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    }

    async function downloadSelected() {
        const paths = selectedPaths();
        const errors = [];
        for (const p of paths) {
            const name = p.split('/').pop();
            const entry = entries.find((e) => entryName(e) === name);
            if (!entry || entry.directory) {
                continue;
            }
            try {
                await downloadFile(p);
            } catch (e) {
                errors.push(`${name}: ${e.message || '下载失败'}`);
            }
        }
        if (errors.length) {
            alert(`部分下载失败：\n${errors.join('\n')}`);
        }
    }

    async function mkdir() {
        await ensureWritable('新建文件夹');
        const name = prompt('文件夹名称');
        if (!name) return;
        await api('/api/files/mkdir', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({path: currentPath, name})
        });
        await loadList();
    }

    async function deleteSelected() {
        await ensureWritable('删除文件');
        const paths = selectedPaths();
        if (!paths.length || !confirm('确认删除选中项？')) return;

        const errors = [];
        for (const p of paths) {
            try {
                await api(`/api/files?path=${encodeURIComponent(p)}`, {method: 'DELETE'});
            } catch (e) {
                errors.push(`${p.split('/').pop() || p}: ${e.message || '删除失败'}`);
            }
        }
        try {
            await loadList();
        } catch (e) {
            errors.push(e.message || '刷新列表失败');
        }
        if (errors.length) {
            alert(`部分删除失败：\n${errors.join('\n')}`);
        }
    }

    const toggleAll = (el) => {
        for (const checkbox of document.querySelectorAll('#fileList input[type=checkbox]')) {
            checkbox.checked = el.checked;
        }
    };

    async function bootstrap() {
        initTheme();
        try {
            const status = await refreshAuthStatus();
            scheduleAuthRefresh();

            if (status.loggedIn || !authRequired) {
                await loadList();
            }
        } catch (e) {
            showLogin();
            const errEl = $('loginError');
            errEl.textContent = e.message || '无法连接服务';
            errEl.style.display = 'block';
        }
    }

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            refreshAuthStatus().catch(() => { /* ignore */
            });
        }
    });

    $('btnTheme').addEventListener('click', toggleTheme);
    $('btnThemeLogin').addEventListener('click', toggleTheme);
    $('btnLogin').addEventListener('click', login);
    $('password').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') login();
    });
    $('btnLogout').addEventListener('click', logout);
    $('btnUpload').addEventListener('click', () => $('fileInput').click());
    $('emptyUploadLink').addEventListener('click', (e) => {
        e.preventDefault();
        $('fileInput').click();
    });
    $('emptyMkdirLink').addEventListener('click', (e) => {
        e.preventDefault();
        mkdir().catch(showError);
    });
    $('fileInput').addEventListener('change', (e) => {
        uploadFiles(e.target.files).catch(showError);
    });
    $('btnDownload').addEventListener('click', () => downloadSelected().catch(showError));
    $('btnMkdir').addEventListener('click', () => mkdir().catch(showError));
    $('btnDelete').addEventListener('click', () => deleteSelected().catch(showError));
    $('selectAll').addEventListener('change', (e) => toggleAll(e.target));
    $('breadcrumb').addEventListener('click', navigate);
    $('btnHideProgress').addEventListener('click', () => {
        $('progressPanel').style.display = 'none';
    });

    bootstrap();
})();
