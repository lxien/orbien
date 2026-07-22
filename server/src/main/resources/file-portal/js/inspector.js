(() => {
    'use strict';

    const PREVIEW_UNKNOWN_ICON = '/icon/preview-unknown.svg';
    const TEXT_PREVIEW_MAX_LINES = 500;

    let abortController = null;
    let blobUrls = [];

    const revokeBlobUrls = () => {
        for (const url of blobUrls) {
            URL.revokeObjectURL(url);
        }
        blobUrls = [];
    };

    const cancelPending = () => {
        abortController?.abort();
        abortController = new AbortController();
        revokeBlobUrls();
        return abortController.signal;
    };

    const trackBlobUrl = (url) => {
        blobUrls.push(url);
        return url;
    };

    const formatSize = (n) => {
        if (!n) return '—';
        if (n < 1024) return `${n} B`;
        if (n < 1_048_576) return `${(n / 1024).toFixed(1)} KB`;
        if (n < 1_073_741_824) return `${(n / 1_048_576).toFixed(1)} MB`;
        return `${(n / 1_073_741_824).toFixed(1)} GB`;
    };

    const formatTime = (ms) => (ms ? new Date(Number(ms)).toLocaleString('zh-CN') : '—');

    const formatPixels = (w, h) => (w > 0 && h > 0 ? `${w} × ${h}` : '—');

    const formatDpi = (x, y) => (x > 0 && y > 0
        ? `${Math.round(x)} × ${Math.round(y)}`
        : '—');

    const kindLabel = (meta) => {
        if (meta.directory) return '文件夹';
        const ext = (meta.name || '').split('.').pop();
        if (ext && ext !== meta.name) return `${ext.toUpperCase()} 文件`;
        return '文件';
    };

    const readUint32BE = (view, offset) => view.getUint32(offset, false);

    // JPEG JFIF / EXIF、PNG pHYs；解析失败返回 null
    const parseImageDpi = (buffer) => {
        const bytes = new Uint8Array(buffer);
        if (bytes.length < 24) {
            return null;
        }
        if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4E && bytes[3] === 0x47) {
            return parsePngDpi(bytes);
        }
        if (bytes[0] === 0xFF && bytes[1] === 0xD8) {
            return parseJpegDpi(bytes);
        }
        return null;
    };

    const parsePngDpi = (bytes) => {
        let offset = 8;
        while (offset + 12 <= bytes.length) {
            const length = ((bytes[offset] << 24) | (bytes[offset + 1] << 16)
                | (bytes[offset + 2] << 8) | bytes[offset + 3]) >>> 0;
            const type = String.fromCharCode(
                bytes[offset + 4], bytes[offset + 5], bytes[offset + 6], bytes[offset + 7]);
            const dataStart = offset + 8;
            if (dataStart + length + 4 > bytes.length) {
                break;
            }
            if (type === 'pHYs' && length >= 9) {
                const view = new DataView(bytes.buffer, bytes.byteOffset + dataStart, length);
                const ppux = readUint32BE(view, 0);
                const ppuy = readUint32BE(view, 4);
                const unit = bytes[dataStart + 8];
                if (unit === 1 && ppux > 0 && ppuy > 0) {
                    return {dpiX: ppux * 0.0254, dpiY: ppuy * 0.0254};
                }
                return null;
            }
            if (type === 'IDAT' || type === 'IEND') {
                break;
            }
            offset = dataStart + length + 4;
        }
        return null;
    };

    const parseJpegDpi = (bytes) => {
        let offset = 2;
        while (offset + 4 <= bytes.length) {
            if (bytes[offset] !== 0xFF) {
                break;
            }
            const marker = bytes[offset + 1];
            if (marker === 0xD9 || marker === 0xDA) {
                break;
            }
            const size = (bytes[offset + 2] << 8) | bytes[offset + 3];
            if (size < 2 || offset + 2 + size > bytes.length) {
                break;
            }
            const dataStart = offset + 4;
            const segLen = size - 2;
            if (marker === 0xE0 && segLen >= 12
                && bytes[dataStart] === 0x4A && bytes[dataStart + 1] === 0x46
                && bytes[dataStart + 2] === 0x49 && bytes[dataStart + 3] === 0x46
                && bytes[dataStart + 4] === 0x00) {
                const unit = bytes[dataStart + 7];
                const dx = (bytes[dataStart + 8] << 8) | bytes[dataStart + 9];
                const dy = (bytes[dataStart + 10] << 8) | bytes[dataStart + 11];
                if (dx > 0 && dy > 0) {
                    if (unit === 1) {
                        return {dpiX: dx, dpiY: dy};
                    }
                    if (unit === 2) {
                        return {dpiX: dx * 2.54, dpiY: dy * 2.54};
                    }
                }
            }
            if (marker === 0xE1 && segLen > 8
                && bytes[dataStart] === 0x45 && bytes[dataStart + 1] === 0x78
                && bytes[dataStart + 2] === 0x69 && bytes[dataStart + 3] === 0x66
                && bytes[dataStart + 4] === 0x00 && bytes[dataStart + 5] === 0x00) {
                const dpi = parseExifDpi(bytes, dataStart + 6, segLen - 6);
                if (dpi) {
                    return dpi;
                }
            }
            offset += 2 + size;
        }
        return null;
    };

    const parseExifDpi = (bytes, tiffStart, maxLen) => {
        if (maxLen < 8) {
            return null;
        }
        const view = new DataView(bytes.buffer, bytes.byteOffset + tiffStart, maxLen);
        const endian = String.fromCharCode(bytes[tiffStart], bytes[tiffStart + 1]);
        const le = endian === 'II';
        if (!le && endian !== 'MM') {
            return null;
        }
        const u16 = (off) => le ? view.getUint16(off, true) : view.getUint16(off, false);
        const u32 = (off) => le ? view.getUint32(off, true) : view.getUint32(off, false);
        if (u16(2) !== 42) {
            return null;
        }
        let ifd = u32(4);
        if (ifd + 2 > maxLen) {
            return null;
        }
        const count = u16(ifd);
        let xResOff = 0;
        let yResOff = 0;
        let unit = 2;
        for (let i = 0; i < count; i++) {
            const entry = ifd + 2 + i * 12;
            if (entry + 12 > maxLen) {
                break;
            }
            const tag = u16(entry);
            const type = u16(entry + 2);
            const valueOrOffset = u32(entry + 8);
            if (tag === 0x011A && type === 5 && valueOrOffset + 8 <= maxLen) {
                xResOff = valueOrOffset;
            } else if (tag === 0x011B && type === 5 && valueOrOffset + 8 <= maxLen) {
                yResOff = valueOrOffset;
            } else if (tag === 0x0128 && type === 3) {
                unit = u16(entry + 8);
            }
        }
        if (!xResOff || !yResOff) {
            return null;
        }
        const readRational = (off) => {
            const num = u32(off);
            const den = u32(off + 4);
            return den ? num / den : 0;
        };
        let dx = readRational(xResOff);
        let dy = readRational(yResOff);
        if (!(dx > 0) || !(dy > 0)) {
            return null;
        }
        // 3 = centimeters
        if (unit === 3) {
            dx *= 2.54;
            dy *= 2.54;
        }
        return {dpiX: dx, dpiY: dy};
    };

    const fetchPreviewMeta = async (path, signal) => {
        const res = await fetch(`/api/files/preview-meta?path=${encodeURIComponent(path)}`, {
            credentials: 'include',
            signal
        });
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            throw new Error(data.message || `请求失败: ${res.status}`);
        }
        return res.json();
    };

    const fetchPreviewBlob = async (path, signal) => {
        const res = await fetch(`/api/files/preview?path=${encodeURIComponent(path)}`, {
            credentials: 'include',
            signal
        });
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            throw new Error(data.message || `预览失败: ${res.status}`);
        }
        const truncated = res.headers.get('X-Preview-Truncated') === 'true';
        const blob = await res.blob();
        return {blob, truncated};
    };

    const renderLoading = (previewEl) => {
        previewEl.replaceChildren();
        const loading = document.createElement('div');
        loading.className = 'inspector-preview-loading';
        loading.textContent = '加载中…';
        previewEl.appendChild(loading);
    };

    const renderUnknown = (previewEl, meta) => {
        previewEl.replaceChildren();
        const wrap = document.createElement('div');
        wrap.className = 'inspector-preview-unknown';

        const img = document.createElement('img');
        img.className = 'inspector-preview-unknown-icon';
        img.src = PREVIEW_UNKNOWN_ICON;
        img.alt = '';
        wrap.appendChild(img);

        const ext = document.createElement('div');
        ext.className = 'inspector-preview-unknown-ext';
        const dot = (meta.name || '').lastIndexOf('.');
        ext.textContent = dot > 0 ? meta.name.slice(dot + 1).toUpperCase() : 'FILE';
        wrap.appendChild(ext);

        previewEl.appendChild(wrap);
    };

    const renderImage = async (previewEl, path, meta, signal) => {
        const {blob} = await fetchPreviewBlob(path, signal);
        if (signal.aborted) {
            return null;
        }
        let dpi = null;
        try {
            dpi = parseImageDpi(await blob.arrayBuffer());
        } catch {
            dpi = null;
        }
        const url = trackBlobUrl(URL.createObjectURL(blob));
        previewEl.replaceChildren();
        const img = document.createElement('img');
        img.className = 'inspector-preview-image';
        img.alt = meta.name || '';
        previewEl.appendChild(img);

        const metrics = await new Promise((resolve) => {
            img.onload = () => resolve({
                width: img.naturalWidth || 0,
                height: img.naturalHeight || 0,
                dpiX: dpi?.dpiX || 0,
                dpiY: dpi?.dpiY || 0
            });
            img.onerror = () => resolve({
                width: 0,
                height: 0,
                dpiX: dpi?.dpiX || 0,
                dpiY: dpi?.dpiY || 0
            });
            img.src = url;
        });
        return signal.aborted ? null : metrics;
    };

    const renderText = async (previewEl, path, meta, signal) => {
        const {blob, truncated} = await fetchPreviewBlob(path, signal);
        const text = await blob.text();
        previewEl.replaceChildren();

        const pre = document.createElement('pre');
        pre.className = 'inspector-preview-text';
        const lines = text.split('\n');
        const clipped = lines.length > TEXT_PREVIEW_MAX_LINES;
        const display = clipped ? lines.slice(0, TEXT_PREVIEW_MAX_LINES).join('\n') : text;
        pre.textContent = display;
        previewEl.appendChild(pre);

        if (truncated || clipped) {
            const note = document.createElement('p');
            note.className = 'inspector-preview-truncated';
            note.textContent = truncated
                ? '文件较大，仅显示部分内容'
                : `文本过长，仅显示前 ${TEXT_PREVIEW_MAX_LINES} 行`;
            previewEl.appendChild(note);
        }
    };

    const renderFolderIcon = (previewEl, iconUrl) => {
        previewEl.replaceChildren();
        const wrap = document.createElement('div');
        wrap.className = 'inspector-preview-folder';
        const img = document.createElement('img');
        img.src = iconUrl || '/icon/folder.svg';
        img.alt = '';
        wrap.appendChild(img);
        previewEl.appendChild(wrap);
    };

    const renderDetails = (detailsEl, meta, imageMetrics = null) => {
        detailsEl.replaceChildren();

        const title = document.createElement('h3');
        title.className = 'inspector-details-title';
        title.textContent = meta.name || '—';
        detailsEl.appendChild(title);

        const subtitle = document.createElement('p');
        subtitle.className = 'inspector-details-subtitle';
        subtitle.textContent = meta.directory
            ? '文件夹'
            : `${kindLabel(meta)} · ${formatSize(meta.size)}`;
        detailsEl.appendChild(subtitle);

        const section = document.createElement('div');
        section.className = 'inspector-details-section';

        const heading = document.createElement('div');
        heading.className = 'inspector-details-heading';
        heading.textContent = '信息';
        section.appendChild(heading);

        const rows = [
            ['创建时间', formatTime(meta.createdTime)],
            ['修改时间', formatTime(meta.modifiedTime)],
            ['上次打开', formatTime(meta.lastAccessTime)]
        ];
        if (!meta.directory) {
            rows.push(['大小', formatSize(meta.size)]);
        }
        if ((meta.previewKind || '').toLowerCase() === 'image' && imageMetrics) {
            if (imageMetrics.width > 0 && imageMetrics.height > 0) {
                rows.push(['尺寸', formatPixels(imageMetrics.width, imageMetrics.height)]);
            }
            if (imageMetrics.dpiX > 0 && imageMetrics.dpiY > 0) {
                rows.push(['分辨率', formatDpi(imageMetrics.dpiX, imageMetrics.dpiY)]);
            }
        }

        for (const [label, value] of rows) {
            const row = document.createElement('div');
            row.className = 'inspector-details-row';
            const lbl = document.createElement('span');
            lbl.className = 'inspector-details-label';
            lbl.textContent = label;
            const val = document.createElement('span');
            val.className = 'inspector-details-value';
            val.textContent = value;
            row.append(lbl, val);
            section.appendChild(row);
        }
        detailsEl.appendChild(section);
    };

    const renderPreview = async (previewEl, path, meta, signal) => {
        if (meta.directory) {
            renderFolderIcon(previewEl, '/icon/folder.svg');
            return null;
        }
        const kind = (meta.previewKind || '').toLowerCase();
        if (kind === 'image' && meta.previewable) {
            return renderImage(previewEl, path, meta, signal);
        }
        if (kind === 'text' && meta.previewable) {
            await renderText(previewEl, path, meta, signal);
            return null;
        }
        renderUnknown(previewEl, meta);
        return null;
    };

    const buildInspectorPanel = (mode = 'column') => {
        const panel = document.createElement('div');
        panel.className = `inspector-panel is-${mode}`;

        const preview = document.createElement('div');
        preview.className = 'inspector-preview';
        panel.appendChild(preview);

        const details = document.createElement('div');
        details.className = 'inspector-details';
        panel.appendChild(details);

        return {panel, preview, details};
    };

    const show = async (container, path, entry, options = {}) => {
        if (!container || !path) {
            return;
        }
        const mode = options.mode || (container.closest('.inspector-dialog-panel') ? 'dialog' : 'column');
        const signal = cancelPending();
        const {panel, preview, details} = buildInspectorPanel(mode);
        container.replaceChildren(panel);
        container.classList.remove('hidden');

        const metaFromEntry = {
            path,
            name: entry?.name || entry?.name_ || path.split('/').pop() || '',
            directory: !!entry?.directory,
            size: entry?.size || 0,
            modifiedTime: entry?.modifiedTime || entry?.modified_time,
            createdTime: entry?.createdTime || entry?.created_time,
            lastAccessTime: entry?.lastAccessTime || entry?.last_access_time
        };

        renderLoading(preview);
        renderDetails(details, metaFromEntry);

        try {
            const meta = await fetchPreviewMeta(path, signal);
            if (signal.aborted) {
                return;
            }
            renderDetails(details, meta);
            const imageMetrics = await renderPreview(preview, path, meta, signal);
            if (signal.aborted) {
                return;
            }
            if (imageMetrics) {
                renderDetails(details, meta, imageMetrics);
            }
        } catch (e) {
            if (signal.aborted || e.name === 'AbortError') {
                return;
            }
            preview.replaceChildren();
            const err = document.createElement('p');
            err.className = 'inspector-preview-error';
            err.textContent = e.message || '预览加载失败';
            preview.appendChild(err);
            if (!options.silent) {
                console.warn('preview failed', e);
            }
        }
    };

    const hide = (container) => {
        cancelPending();
        if (container) {
            container.replaceChildren();
        }
    };

    const openDialog = async (path, entry) => {
        const root = document.getElementById('inspectorDialog');
        const content = document.getElementById('inspectorDialogContent');
        if (!root || !content) {
            return;
        }
        root.classList.remove('hidden');
        root.setAttribute('aria-hidden', 'false');
        await show(content, path, entry, {mode: 'dialog'});
    };

    const closeDialog = () => {
        const root = document.getElementById('inspectorDialog');
        const content = document.getElementById('inspectorDialogContent');
        if (root) {
            root.classList.add('hidden');
            root.setAttribute('aria-hidden', 'true');
        }
        hide(content);
        window.dispatchEvent(new CustomEvent('inspector-dialog-close'));
    };

    const initDialog = () => {
        const root = document.getElementById('inspectorDialog');
        if (!root) {
            return;
        }
        root.querySelectorAll('[data-inspector-dismiss="true"]').forEach((el) => {
            el.addEventListener('click', closeDialog);
        });
        document.getElementById('inspectorDialogClose')?.addEventListener('click', closeDialog);
        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Escape' || root.classList.contains('hidden')) {
                return;
            }
            const appDialog = document.getElementById('appDialog');
            if (appDialog && !appDialog.classList.contains('hidden')) {
                return;
            }
            closeDialog();
        });
    };

    initDialog();
    window.FileInspector = {show, hide, cancelPending, openDialog, closeDialog};
})();
