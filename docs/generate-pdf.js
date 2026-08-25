/**
 * Generates docs/ASSIGNMENT_REPORT.pdf from docs/ASSIGNMENT_REPORT.md,
 * formatted to the assignment brief's exact spec: A4 paper, margins 1.5in
 * left / 1in right-top-bottom, 1.5 line spacing, Times New Roman, headings
 * 14pt bold, body 12pt, page numbers bottom-right.
 *
 * Also prepends, ahead of the report body: an exact copy of the official
 * university cover sheet (rendered as images - see docs/assets/, produced
 * from Assignment cover sheet.docx via Word/XPS, not retyped), a title/
 * cover page, and a navigable Table of Contents built from the report's
 * own H2-H4 headings with real clickable in-PDF links (Chrome's print
 * engine preserves <a href="#slug"> as internal PDF navigation links).
 *
 * One-off export tool, not part of the running application - see
 * docs/SETUP.md "Regenerating the PDF report" for how/when to re-run it.
 *
 * Usage:  npm install   (first time only, installs marked + puppeteer-core)
 *         node generate-pdf.js
 * Requires a local Chrome/Edge install (path below); override with the
 * CHROME_PATH environment variable if yours is elsewhere.
 */
const fs = require('fs');
const path = require('path');

const DOCS_DIR = __dirname;
const MD_PATH = path.join(DOCS_DIR, 'ASSIGNMENT_REPORT.md');
const OUT_PDF = path.join(DOCS_DIR, 'ASSIGNMENT_REPORT.pdf');
const CHROME_PATH = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe';

// Sunrise Dental Clinic's own brand palette (frontend/assets/css/styles.css),
// reused here so the cover/title page matches the rest of the project
// instead of introducing an unrelated colour scheme.
const BRAND = {
    navy: '#0f5c5c',
    navyDark: '#0b4444',
    teal: '#1aa89a',
    amber: '#f2a541',
    text: '#1c2b2b',
    muted: '#5b7877'
};

function toFileUrl(absPath) {
    return 'file:///' + absPath.replace(/\\/g, '/');
}

function slugify(text) {
    return text
        .toLowerCase()
        .replace(/<[^>]+>/g, '')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
}

/** Adds a unique id="" to every <h1>-<h6> in the HTML and returns
 *  { html, headings: [{ depth, id, text }] } (text is plain, tags stripped). */
function addHeadingIds(html) {
    const headings = [];
    const seen = new Map();
    const out = html.replace(/<h([1-6])>([\s\S]*?)<\/h\1>/g, (match, depth, inner) => {
        const plainText = inner.replace(/<[^>]+>/g, '').trim();
        let slug = slugify(plainText) || 'section';
        const count = seen.get(slug) || 0;
        seen.set(slug, count + 1);
        if (count > 0) slug = `${slug}-${count + 1}`;
        headings.push({ depth: Number(depth), id: slug, text: plainText });
        return `<h${depth} id="${slug}">${inner}</h${depth}>`;
    });
    return { html: out, headings };
}

/** Builds a nested TOC <ul> from headings at depth 2-4 (the H1 is the
 *  report's own title, not a section to navigate to). `pageOf(id)`, if
 *  given, supplies the resolved page number to print after each entry
 *  (second pass only - the first pass has no page numbers yet, since the
 *  PDF doesn't exist to resolve them from). */
function buildTocHtml(headings, pageOf) {
    const entries = headings.filter(h => h.depth >= 2 && h.depth <= 4);
    let html = '';
    let currentDepth = 2;
    const openLevels = [];
    for (const h of entries) {
        while (currentDepth < h.depth) { html += '<ul>'; openLevels.push(currentDepth); currentDepth++; }
        while (currentDepth > h.depth) { html += '</ul>'; currentDepth--; openLevels.pop(); }
        const pageNum = pageOf ? pageOf(h.id) : null;
        const pageSpan = pageNum ? `<span class="toc-page">${pageNum}</span>` : '';
        html += `<li><a href="#${h.id}"><span class="toc-text">${h.text}</span>${pageSpan}</a></li>`;
    }
    while (openLevels.length) { html += '</ul>'; openLevels.pop(); }
    return `<ul class="toc-root">${html}</ul>`;
}

/** Second-pass helper: opens a just-generated PDF and, for each link
 *  annotation on its early pages (i.e. the TOC's own links, in the same
 *  order buildTocHtml() emitted them), resolves which page it points to.
 *  Returns an array of 1-based page numbers, same order/length as the
 *  depth-2..4 heading entries. */
async function resolveTocPageNumbers(pdfPath, expectedCount) {
    const pdfjsLib = require('pdfjs-dist/legacy/build/pdf.js');
    const data = new Uint8Array(fs.readFileSync(pdfPath));
    const pdfDoc = await pdfjsLib.getDocument({ data }).promise;

    const pages = [];
    for (let p = 1; p <= pdfDoc.numPages && pages.length < expectedCount; p++) {
        const page = await pdfDoc.getPage(p);
        const annots = await page.getAnnotations();
        for (const a of annots) {
            if (a.subtype === 'Link' && a.dest) {
                let dest = a.dest;
                if (typeof dest === 'string') dest = await pdfDoc.getDestination(dest);
                if (!dest || !dest[0]) continue;
                const pageIndex = await pdfDoc.getPageIndex(dest[0]);
                pages.push(pageIndex + 1);
            }
        }
    }
    return pages;
}

/** Builds the full HTML document and prints it to OUT_PDF. `pageOf`, if
 *  given, is a Map from heading id -> resolved page number, used to print
 *  real page numbers next to each TOC entry (second pass only). */
async function buildAndPrint({ marked, puppeteer, pageOf } = {}) {
    let md = fs.readFileSync(MD_PATH, 'utf8');
    let bodyHtml = marked.parse(md);

    bodyHtml = bodyHtml.replace(/<img src="([^"]+)"/g, (m, src) => {
        if (/^https?:\/\//.test(src) || /^file:\/\//.test(src)) return m;
        const abs = path.resolve(DOCS_DIR, src);
        return `<img src="${toFileUrl(abs)}"`;
    });

    const { html: bodyHtmlWithIds, headings } = addHeadingIds(bodyHtml);
    const tocHtml = buildTocHtml(headings, pageOf ? (id => pageOf.get(id)) : null);

    const coverPage1 = toFileUrl(path.join(DOCS_DIR, 'assets', 'cover-sheet-page-1.png'));
    const coverPage2 = toFileUrl(path.join(DOCS_DIR, 'assets', 'cover-sheet-page-2.png'));

    const frontMatter = `
<div class="cover-sheet-page"><img src="${coverPage1}" alt="Assignment Cover Sheet, page 1"></div>
<div class="pagebreak"></div>
<div class="cover-sheet-page"><img src="${coverPage2}" alt="Assignment Cover Sheet, page 2"></div>
<div class="pagebreak"></div>

<div class="title-page">
  <p class="tp-university">International College of Business and Technology (ICBT) &ndash; Jaffna</p>
  <p class="tp-affiliation">Cardiff Metropolitan University</p>
  <div class="tp-title-block">
    <h1 class="tp-title">Sunrise Dental Clinic System</h1>
    <p class="tp-subtitle">Online Appointment &amp; Patient Management System</p>
    <p class="tp-assessment">Assignment Report &mdash; CIS6003 Advanced Programming (WRIT1)</p>
  </div>
  <div class="tp-accent-bar"></div>
  <p class="tp-desc">A full-stack, three-tier web application built to replace Sunrise Dental
  Clinic's paper-based booking process: a Spring Boot REST API secured with JWT
  authentication, a MySQL database with triggers/a stored procedure/views, five
  GoF design patterns, and a static HTML/CSS/JS client &mdash; covering
  appointment booking, patient records, dentist scheduling, and automated
  billing end-to-end.</p>
  <p class="tp-date">September 5, 2026</p>
  <div class="tp-student-block">
    <p><strong>Student Name:</strong> Kirisha</p>
    <p><strong>Student ID:</strong> JF/BSCSD/19/33</p>
    <p class="tp-course"><strong>Course:</strong> BSc SE (Top-Up)</p>
  </div>
</div>
<div class="pagebreak"></div>

<div class="toc-page">
  <h1 class="toc-heading">Table of Contents</h1>
  ${tocHtml}
</div>
<div class="pagebreak"></div>
`;

    const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>CIS6003 Assignment Report</title>
<style>
  @page { size: A4; margin: 1in 1in 1in 1.5in; }
  * { box-sizing: border-box; }
  body {
    font-family: "Times New Roman", Times, serif;
    font-size: 12pt;
    line-height: 1.5;
    color: #000;
  }
  h1, h2, h3, h4, h5, h6 {
    font-family: "Times New Roman", Times, serif;
    font-size: 14pt;
    font-weight: bold;
    margin-top: 18pt;
    margin-bottom: 8pt;
    page-break-after: avoid;
  }
  h1 { border-bottom: 1.5pt solid #0b3954; padding-bottom: 6pt; }
  p, li { text-align: justify; }
  a { color: #0b3954; }
  hr { border: none; border-top: 0.75pt solid #999; margin: 14pt 0; }
  /* Every element below is deliberately kept at the brief's exact spec -
     12pt Times New Roman, 1.5 line spacing - with NO exceptions for
     tables/blockquotes/inline code, since the brief states this plainly
     for the whole document and tables are explicitly named as counted,
     "normal" report content in the brief's word-count clause. Only figure
     captions (img + em below) are kept as a smaller, conventional caption
     style, since the brief's format spec targets the report's body text
     and does not address figure/photo captions. */
  blockquote {
    border-left: 3pt solid #999;
    margin: 10pt 0;
    padding: 2pt 10pt;
    color: #333;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    margin: 10pt 0;
    font-size: 12pt;
    line-height: 1.5;
  }
  th, td {
    border: 0.75pt solid #888;
    padding: 4pt 6pt;
    text-align: left;
    vertical-align: top;
  }
  th { background: #eef3f6; font-weight: bold; }
  code {
    font-family: "Times New Roman", Times, serif;
    font-size: 12pt;
    background: #f2f2f2;
    padding: 1pt 3pt;
    border-radius: 2pt;
  }
  pre {
    background: #f2f2f2;
    padding: 8pt;
    font-size: 12pt;
    line-height: 1.5;
    overflow-x: auto;
    page-break-inside: avoid;
  }
  pre code { background: none; padding: 0; }
  img {
    max-width: 100%;
    height: auto;
    display: block;
    margin: 10pt auto;
    page-break-inside: avoid;
    border: 0.75pt solid #ccc;
  }
  .pagebreak { page-break-after: always; }

  /* ---- Cover sheet (exact copy, no added border/decoration) ---- */
  .cover-sheet-page { text-align: center; }
  .cover-sheet-page img { max-width: 100%; height: auto; border: none; margin: 0 auto; }

  /* ---- Title / cover page ---- */
  .title-page { text-align: center; padding-top: 0.4in; }
  .tp-university {
    color: ${BRAND.amber}; font-weight: bold; font-size: 13pt;
    letter-spacing: 0.5pt; margin-bottom: 2pt;
  }
  .tp-affiliation { color: ${BRAND.muted}; font-style: italic; font-size: 12pt; margin-top: 0; }
  .tp-title-block { margin-top: 0.7in; }
  .tp-title { color: ${BRAND.navyDark}; font-size: 30pt; border: none; padding: 0; margin: 0; }
  .tp-subtitle { color: ${BRAND.teal}; font-size: 16pt; font-weight: bold; margin: 6pt 0 0; }
  .tp-assessment { color: ${BRAND.muted}; font-style: italic; font-size: 12.5pt; margin: 4pt 0 0; }
  .tp-accent-bar {
    width: 100%; height: 6pt; margin: 0.35in 0;
    background: linear-gradient(90deg, ${BRAND.navy}, ${BRAND.teal} 55%, ${BRAND.amber});
  }
  .tp-desc {
    text-align: left; color: ${BRAND.text}; font-size: 11.5pt; font-style: italic;
    max-width: 90%; margin: 0 auto 0.35in;
  }
  .tp-date { color: ${BRAND.text}; font-size: 12.5pt; margin-bottom: 0.9in; }
  .tp-student-block { text-align: left; font-size: 12pt; }
  .tp-student-block p { margin: 2pt 0; }
  .tp-course { color: ${BRAND.amber}; }

  /* ---- Table of Contents ---- */
  .toc-heading { border: none; text-align: center; color: ${BRAND.navyDark}; }
  .toc-root, .toc-root ul { list-style: none; padding-left: 0; margin: 0; }
  .toc-root > li { margin-top: 10pt; }
  .toc-root ul { padding-left: 22pt; }
  .toc-root ul li { margin-top: 4pt; }
  .toc-root a {
    text-decoration: none; color: ${BRAND.text};
    display: flex; align-items: baseline;
  }
  .toc-root > li > a { font-weight: bold; color: ${BRAND.navyDark}; }
  .toc-text { flex: 1; display: flex; align-items: baseline; }
  .toc-text::after {
    content: ""; flex: 1; margin: 0 6pt; border-bottom: 0.75pt dotted #999; transform: translateY(-3pt);
  }
  .toc-page { flex: none; }
</style>
</head>
<body>
${frontMatter}
${bodyHtmlWithIds}
</body>
</html>`;

    const tmpHtmlPath = path.join(DOCS_DIR, '_report_render.tmp.html');
    fs.writeFileSync(tmpHtmlPath, html, 'utf8');

    const browser = await puppeteer.launch({ executablePath: CHROME_PATH, headless: 'new' });
    const page = await browser.newPage();
    await page.goto(toFileUrl(tmpHtmlPath), { waitUntil: 'networkidle0', timeout: 60000 });

    await page.pdf({
        path: OUT_PDF,
        format: 'A4',
        printBackground: true,
        margin: { top: '1in', right: '1in', bottom: '1in', left: '1.5in' },
        displayHeaderFooter: true,
        headerTemplate: '<span></span>',
        footerTemplate: `
          <div style="width:100%; font-size:9pt; font-family:'Times New Roman',serif; text-align:right; padding-right:1in;">
            <span class="pageNumber"></span>
          </div>`
    });

    await browser.close();
    fs.unlinkSync(tmpHtmlPath);
    return headings;
}

async function main() {
    const { marked } = await import('marked');
    const puppeteer = (await import('puppeteer-core')).default;
    marked.setOptions({ gfm: true, breaks: false });

    // Pass 1: render without TOC page numbers (they don't exist yet - the
    // PDF itself is what tells us where each heading actually landed).
    const headings = await buildAndPrint({ marked, puppeteer });
    const tocEntries = headings.filter(h => h.depth >= 2 && h.depth <= 4);

    // Resolve real page numbers from the pass-1 PDF's own TOC link
    // annotations, then re-render with those numbers included.
    const resolvedPages = await resolveTocPageNumbers(OUT_PDF, tocEntries.length);
    if (resolvedPages.length === tocEntries.length) {
        const pageOf = new Map(tocEntries.map((h, i) => [h.id, resolvedPages[i]]));
        await buildAndPrint({ marked, puppeteer, pageOf });
        console.log('Generated', OUT_PDF, `(${headings.length} headings indexed in the TOC, with resolved page numbers)`);
    } else {
        console.warn(`Could not resolve TOC page numbers (found ${resolvedPages.length}, expected ${tocEntries.length}) - keeping the unnumbered TOC.`);
        console.log('Generated', OUT_PDF, `(${headings.length} headings indexed in the TOC)`);
    }
}

main().catch(err => { console.error(err); process.exit(1); });
