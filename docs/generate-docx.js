/**
 * Generates docs/ASSIGNMENT_REPORT.docx from docs/ASSIGNMENT_REPORT.md,
 * formatted to the assignment brief's exact spec: A4 paper, margins 1.5in
 * left / 1in right-top-bottom, 1.5 line spacing, Times New Roman, headings
 * 14pt bold, body 12pt, page numbers bottom-right.
 *
 * Parses the Markdown via `marked`'s lexer (a structured token tree) and
 * walks it to build native docx.js elements (Paragraph/Table/ImageRun),
 * rather than converting through HTML, so headings, lists, tables and
 * images all become real Word constructs, not an HTML paste-in.
 *
 * One-off export tool, not part of the running application - see
 * docs/SETUP.md "Regenerating the PDF/Word report".
 *
 * Usage: npm install   (first time only)
 *        node generate-docx.js
 */
const fs = require('fs');
const path = require('path');
const {
    Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType,
    Table, TableRow, TableCell, WidthType, BorderStyle, ShadingType,
    ImageRun, Header, Footer, PageNumber, ExternalHyperlink, VerticalAlign,
    convertInchesToTwip, LevelFormat, TabStopType, PageBreak, TableLayoutType
} = require('docx');

const DOCS_DIR = __dirname;
const MD_PATH = path.join(DOCS_DIR, 'ASSIGNMENT_REPORT.md');
const OUT_PATH = path.join(DOCS_DIR, 'ASSIGNMENT_REPORT.docx');

const FONT = 'Times New Roman';
const BODY_SIZE = 24;     // half-points -> 12pt
const HEADING_SIZE = 28;  // half-points -> 14pt
const MONO_FONT = 'Consolas';
const LINE_1_5 = 360;     // docx line spacing units, 240 = single -> 360 = 1.5x
const MAX_IMG_WIDTH_PX = 560; // page width (A4 8.27in) minus 1.5in+1in margins, at 96dpi

// ---------- PNG dimension reader (no native deps) ----------
function pngDimensions(buf) {
    // PNG signature (8 bytes) then IHDR chunk: length(4) type(4) width(4) height(4)
    const width = buf.readUInt32BE(16);
    const height = buf.readUInt32BE(20);
    return { width, height };
}

function scaledImageSize(width, height) {
    if (width <= MAX_IMG_WIDTH_PX) return { width, height };
    const ratio = MAX_IMG_WIDTH_PX / width;
    return { width: MAX_IMG_WIDTH_PX, height: Math.round(height * ratio) };
}

// ---------- Inline token -> TextRun[] ----------
function inlineRuns(tokens, opts = {}) {
    const runs = [];
    for (const t of tokens || []) {
        switch (t.type) {
            case 'text':
            case 'escape':
                if (t.tokens && t.tokens.length) {
                    runs.push(...inlineRuns(t.tokens, opts));
                } else {
                    runs.push(new TextRun({ text: t.text, font: FONT, size: BODY_SIZE, ...opts }));
                }
                break;
            case 'strong':
                runs.push(...inlineRuns(t.tokens, { ...opts, bold: true }));
                break;
            case 'em':
                runs.push(...inlineRuns(t.tokens, { ...opts, italics: true }));
                break;
            case 'codespan':
                // Kept in the brief's Times New Roman / 12pt spec, not a
                // monospace font - see the file-level note above blockToElements.
                runs.push(new TextRun({
                    text: t.text, font: FONT, size: BODY_SIZE, ...opts,
                    shading: { type: ShadingType.SOLID, color: 'F2F2F2', fill: 'F2F2F2' }
                }));
                break;
            case 'link':
                runs.push(new ExternalHyperlink({
                    link: t.href,
                    children: [new TextRun({
                        text: (t.tokens && t.tokens[0] && t.tokens[0].text) || t.text || t.href,
                        font: FONT, size: BODY_SIZE, color: '0B3954', underline: {}, ...opts
                    })]
                }));
                break;
            case 'br':
                runs.push(new TextRun({ text: '', break: 1 }));
                break;
            case 'del':
                runs.push(...inlineRuns(t.tokens, { ...opts, strike: true }));
                break;
            default:
                if (t.raw) runs.push(new TextRun({ text: t.raw, font: FONT, size: BODY_SIZE, ...opts }));
        }
    }
    return runs;
}

function headingLevelFor(depth) {
    return [null, HeadingLevel.HEADING_1, HeadingLevel.HEADING_2, HeadingLevel.HEADING_3,
        HeadingLevel.HEADING_4, HeadingLevel.HEADING_5, HeadingLevel.HEADING_6][depth];
}

// ---------- Block token -> docx element(s) ----------
function blockToElements(token, listDepth = 0) {
    switch (token.type) {
        case 'heading':
            return [new Paragraph({
                heading: headingLevelFor(Math.min(token.depth, 6)),
                spacing: { before: 240, after: 120, line: LINE_1_5, lineRule: 'auto' },
                border: token.depth === 1 ? { bottom: { style: BorderStyle.SINGLE, size: 6, color: '0B3954' } } : undefined,
                // keepNext ties a heading to the paragraph immediately after
                // it, so Word never leaves a heading alone at the bottom of
                // one page with its explanatory text starting on the next.
                keepNext: true,
                children: inlineRuns(token.tokens, { bold: true, size: HEADING_SIZE })
            })];

        case 'paragraph':
            // A standalone `![alt](src)` line is lexed as a paragraph wrapping a single
            // inline "image" token, not a block-level "image" token - render it as an image.
            if (token.tokens && token.tokens.length === 1 && token.tokens[0].type === 'image') {
                return blockToElements(token.tokens[0], listDepth);
            }
            return [new Paragraph({
                alignment: AlignmentType.JUSTIFIED,
                spacing: { after: 160, line: LINE_1_5, lineRule: 'auto' },
                // keepLines keeps every line of one paragraph together on the
                // same page, rather than letting Word split it mid-paragraph
                // across a page boundary.
                keepLines: true,
                children: inlineRuns(token.tokens)
            })];

        case 'blockquote': {
            const paras = token.tokens.filter(t => t.type === 'paragraph');
            return paras.map(p => new Paragraph({
                indent: { left: 480 },
                border: { left: { style: BorderStyle.SINGLE, size: 12, color: '999999' } },
                spacing: { after: 120, line: LINE_1_5, lineRule: 'auto' },
                children: inlineRuns(p.tokens, { italics: true, color: '333333' })
            }));
        }

        case 'list': {
            const els = [];
            let idx = token.start && Number.isInteger(token.start) ? token.start : 1;
            for (const item of token.items) {
                const itemTokens = item.tokens.filter(t => t.type === 'text' || t.type === 'paragraph');
                const inline = itemTokens.flatMap(t => t.tokens ? inlineRuns(t.tokens) : inlineRuns([t]));
                const prefix = token.ordered ? `${idx}. ` : null;
                els.push(new Paragraph({
                    spacing: { after: 80, line: LINE_1_5, lineRule: 'auto' },
                    indent: { left: 420 + listDepth * 300, hanging: prefix ? 260 : 260 },
                    bullet: prefix ? undefined : { level: listDepth },
                    keepLines: true,
                    children: prefix
                        ? [new TextRun({ text: prefix, font: FONT, size: BODY_SIZE }), ...inline]
                        : inline
                }));
                // nested lists inside this item
                const nested = item.tokens.filter(t => t.type === 'list');
                for (const n of nested) els.push(...blockToElements(n, listDepth + 1));
                idx++;
            }
            return els;
        }

        case 'table': {
            // Table text is deliberately kept at the same 12pt / 1.5-line-spacing
            // as the rest of the report - the brief's word-count clause names
            // tables explicitly as counted, "normal" report content, so they
            // follow the same "Normal 12pt" / 1.5 spacing rule, not a smaller
            // condensed style.
            //
            // Word distributes column widths evenly across a table with no
            // explicit widths, which is fine for tables whose columns hold
            // similarly-sized content but wraps short values (an ID like
            // "AUTH-01", a status like "PASS") into an unreadable single
            // character per line when a neighbouring column (a long test
            // description, a dotted Java method name) needs far more room.
            // Sizing each column from its own typical content length - not
            // one hard-coded shape - fixes this generally, for every table
            // in the report, not just the widest ones.
            const colCount = token.header.length;
            const colWeights = token.header.map((h, i) => {
                const headerLen = (h.text || '').length;
                const cellLens = token.rows.map(r => ((r[i] && r[i].text) || '').length);
                const avgLen = cellLens.length ? cellLens.reduce((a, b) => a + b, 0) / cellLens.length : 0;
                return Math.max(headerLen, avgLen, 4);
            });
            const totalWeight = colWeights.reduce((a, b) => a + b, 0);
            const MIN_PCT = 11;
            let colPct = colWeights.map(w => Math.max((w / totalWeight) * 100, MIN_PCT));
            const pctSum = colPct.reduce((a, b) => a + b, 0);
            colPct = colPct.map(p => Math.round((p / pctSum) * 10000) / 100); // 2dp, sums to ~100

            const headerCells = token.header.map((h, i) => new TableCell({
                shading: { type: ShadingType.SOLID, color: 'EEF3F6', fill: 'EEF3F6' },
                verticalAlign: VerticalAlign.CENTER,
                width: { size: colPct[i], type: WidthType.PERCENTAGE },
                margins: { top: 60, bottom: 60, left: 80, right: 80 },
                children: [new Paragraph({
                    spacing: { line: LINE_1_5, lineRule: 'auto' },
                    children: inlineRuns(h.tokens, { bold: true, size: BODY_SIZE })
                })]
            }));
            const rows = [new TableRow({ children: headerCells, tableHeader: true })];
            for (const row of token.rows) {
                const cells = row.map((c, i) => new TableCell({
                    verticalAlign: VerticalAlign.TOP,
                    width: { size: colPct[Math.min(i, colCount - 1)], type: WidthType.PERCENTAGE },
                    margins: { top: 60, bottom: 60, left: 80, right: 80 },
                    children: [new Paragraph({
                        spacing: { line: LINE_1_5, lineRule: 'auto' },
                        children: inlineRuns(c.tokens, { size: BODY_SIZE })
                    })]
                }));
                rows.push(new TableRow({ children: cells }));
            }
            return [
                // layout: FIXED is required for Word to actually honour the
                // per-column widths above - Word's default AUTOFIT layout
                // silently recalculates every column from its own content
                // instead, which is what produced unreadable single-letter-
                // per-line wrapping in the ID/Type/Status columns before.
                new Table({ width: { size: 100, type: WidthType.PERCENTAGE }, layout: TableLayoutType.FIXED, rows }),
                new Paragraph({ text: '', spacing: { after: 160 } })
            ];
        }

        case 'code': {
            const lines = token.text.split('\n');
            return [new Paragraph({
                shading: { type: ShadingType.SOLID, color: 'F2F2F2', fill: 'F2F2F2' },
                spacing: { after: 160, line: LINE_1_5, lineRule: 'auto' },
                children: lines.flatMap((line, i) => {
                    const run = new TextRun({ text: line || ' ', font: FONT, size: BODY_SIZE });
                    return i < lines.length - 1 ? [run, new TextRun({ text: '', break: 1 })] : [run];
                })
            })];
        }

        case 'image': {
            try {
                const imgPath = /^https?:\/\//.test(token.href) ? null : path.resolve(DOCS_DIR, token.href);
                if (!imgPath || !fs.existsSync(imgPath)) throw new Error('image not found: ' + token.href);
                const buf = fs.readFileSync(imgPath);
                const { width, height } = pngDimensions(buf);
                const size = scaledImageSize(width, height);
                return [
                    new Paragraph({
                        alignment: AlignmentType.CENTER,
                        // Extra top spacing (vs. the previous 120) gives two
                        // back-to-back figures (an image's caption immediately
                        // followed by the next image, with no body text
                        // between them) a visibly clear gap rather than
                        // looking stacked together.
                        spacing: { before: 240, after: 60 },
                        // Keep the image tied to its own caption paragraph
                        // (never split onto different pages from each other).
                        keepNext: true,
                        keepLines: true,
                        children: [new ImageRun({ data: buf, transformation: size, type: 'png' })]
                    }),
                    new Paragraph({
                        alignment: AlignmentType.CENTER,
                        spacing: { after: 240 },
                        keepLines: true,
                        children: [new TextRun({ text: token.text || '', italics: true, size: BODY_SIZE - 4, font: FONT })]
                    })
                ];
            } catch (e) {
                console.error('IMAGE ERROR for', token.href, '->', e.message);
                return [new Paragraph({ children: [new TextRun({ text: `[image missing: ${token.href}]`, italics: true, size: BODY_SIZE })] })];
            }
        }

        case 'hr':
            return [new Paragraph({
                spacing: { before: 120, after: 120 },
                border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '999999' } },
                children: []
            })];

        case 'space':
            return [];

        case 'html':
            // Only recognised raw-HTML passthrough: an explicit page break
            // marker (used once, after the cover sheet). Anything else is
            // intentionally dropped rather than rendered as literal markup.
            if (/class="pagebreak"/.test(token.raw)) {
                return [new Paragraph({ children: [new PageBreak()] })];
            }
            return [];

        default:
            if (token.tokens) return token.tokens.flatMap(t => blockToElements(t, listDepth));
            if (token.raw && token.raw.trim()) {
                return [new Paragraph({ children: [new TextRun({ text: token.raw, font: FONT, size: BODY_SIZE })] })];
            }
            return [];
    }
}

async function main() {
    const { marked } = await import('marked');
    marked.use({ gfm: true });

    const md = fs.readFileSync(MD_PATH, 'utf8');
    const tokens = marked.lexer(md);

    const children = [];
    for (const t of tokens) children.push(...blockToElements(t));

    const doc = new Document({
        styles: {
            default: {
                document: { run: { font: FONT, size: BODY_SIZE } }
            }
        },
        sections: [{
            properties: {
                page: {
                    size: { width: convertInchesToTwip(8.27), height: convertInchesToTwip(11.69) }, // A4
                    margin: {
                        top: convertInchesToTwip(1),
                        right: convertInchesToTwip(1),
                        bottom: convertInchesToTwip(1),
                        left: convertInchesToTwip(1.5)
                    }
                }
            },
            headers: { default: new Header({ children: [new Paragraph({ children: [] })] }) },
            footers: {
                default: new Footer({
                    children: [new Paragraph({
                        alignment: AlignmentType.RIGHT,
                        children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: BODY_SIZE - 4 })]
                    })]
                })
            },
            children
        }]
    });

    const buffer = await Packer.toBuffer(doc);
    fs.writeFileSync(OUT_PATH, buffer);
    console.log('Generated', OUT_PATH);
}

main().catch(err => { console.error(err); process.exit(1); });
