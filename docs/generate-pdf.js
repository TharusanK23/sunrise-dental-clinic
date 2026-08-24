/**
 * Generates docs/ASSIGNMENT_REPORT.pdf from docs/ASSIGNMENT_REPORT.md,
 * formatted to the assignment brief's exact spec: A4 paper, margins 1.5in
 * left / 1in right-top-bottom, 1.5 line spacing, Times New Roman, headings
 * 14pt bold, body 12pt, page numbers bottom-right.
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
const REPO_ROOT = path.resolve(DOCS_DIR, '..');
const MD_PATH = path.join(DOCS_DIR, 'ASSIGNMENT_REPORT.md');
const OUT_PDF = path.join(DOCS_DIR, 'ASSIGNMENT_REPORT.pdf');
const CHROME_PATH = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe';

function toFileUrl(absPath) {
    return 'file:///' + absPath.replace(/\\/g, '/');
}

async function main() {
    const { marked } = await import('marked');
    const puppeteer = (await import('puppeteer-core')).default;

    marked.setOptions({ gfm: true, breaks: false });

    let md = fs.readFileSync(MD_PATH, 'utf8');
    let bodyHtml = marked.parse(md);

    // Resolve every relative image src (as written in the Markdown, relative
    // to docs/) to an absolute file:// URL so the temp HTML can be printed
    // regardless of where it is written from.
    bodyHtml = bodyHtml.replace(/<img src="([^"]+)"/g, (m, src) => {
        if (/^https?:\/\//.test(src) || /^file:\/\//.test(src)) return m;
        const abs = path.resolve(DOCS_DIR, src);
        return `<img src="${toFileUrl(abs)}"`;
    });

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
  h1, h2, h3, h4, h5 {
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
  blockquote {
    border-left: 3pt solid #999;
    margin: 10pt 0;
    padding: 2pt 10pt;
    color: #333;
    font-size: 11pt;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    margin: 10pt 0;
    font-size: 10pt;
    line-height: 1.3;
  }
  th, td {
    border: 0.75pt solid #888;
    padding: 4pt 6pt;
    text-align: left;
    vertical-align: top;
  }
  th { background: #eef3f6; font-weight: bold; }
  code {
    font-family: "Consolas", "Courier New", monospace;
    font-size: 10pt;
    background: #f2f2f2;
    padding: 1pt 3pt;
    border-radius: 2pt;
  }
  pre {
    background: #f2f2f2;
    padding: 8pt;
    font-size: 9.5pt;
    line-height: 1.3;
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
</style>
</head>
<body>
${bodyHtml}
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
    console.log('Generated', OUT_PDF);
}

main().catch(err => { console.error(err); process.exit(1); });
