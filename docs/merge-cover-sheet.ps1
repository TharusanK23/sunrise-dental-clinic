<#
.SYNOPSIS
    Finalises docs/ASSIGNMENT_REPORT.docx: merges in the university's
    official cover sheet as live editable content, adds a title/cover
    page (Sunrise Dental Clinic's own brand colours), and inserts a
    navigable Table of Contents.

.DESCRIPTION
    Run this AFTER `node generate-docx.js` (which produces the report body
    only, starting at "CIS6003 Advanced Programming - Assignment Report").
    Final page order: (1) Assignment Cover Sheet [own Letter-size section,
    exact copy of the original, live/editable], (2) Title/cover page,
    (3) Table of Contents, (4) the report body.

    Note: this script does NOT export to PDF - Word's own PDF export
    (SaveAs2 wdFormatPDF) proved unreliable for this large, image-heavy
    document (repeatedly hung indefinitely). The PDF is instead produced
    entirely by generate-pdf.js (Puppeteer/Chrome), which independently
    builds the same cover sheet + title page + TOC content and has been
    reliable throughout. Run both:

        node generate-docx.js
        powershell -ExecutionPolicy Bypass -File merge-cover-sheet.ps1
        node generate-pdf.js

    Requires Microsoft Word (COM automation) - Windows only, unlike the
    two Node scripts. See docs/SETUP.md.
#>

$ErrorActionPreference = "Stop"

$wdFormatOriginalFormatting = 16
$wdStory = 6
$wdCollapseStart = 1
$wdCollapseEnd = 0
$wdSectionBreakNextPage = 2
$wdPageBreak = 7
$wdAlignParagraphCenter = 1
$wdAlignParagraphLeft = 0

$coverPath = "D:\ICBT\OnlineVehicleReservation\Assignment cover sheet.docx"
$reportPath = Join-Path $PSScriptRoot "ASSIGNMENT_REPORT.docx"

if (-not (Test-Path $coverPath)) {
    throw "Cover sheet not found at $coverPath - update `$coverPath in this script if it has moved."
}
if (-not (Test-Path $reportPath)) {
    throw "ASSIGNMENT_REPORT.docx not found - run 'node generate-docx.js' first."
}

# Sunrise Dental Clinic's own brand palette (frontend/assets/css/styles.css),
# converted to Word's BGR-packed OLE colour integers (r + g*256 + b*65536).
function ToWordColor([string]$hex) {
    $r = [Convert]::ToInt32($hex.Substring(0,2), 16)
    $g = [Convert]::ToInt32($hex.Substring(2,2), 16)
    $b = [Convert]::ToInt32($hex.Substring(4,2), 16)
    return $r + ($g * 256) + ($b * 65536)
}
$colNavy      = ToWordColor "0f5c5c"
$colNavyDark  = ToWordColor "0b4444"
$colTeal      = ToWordColor "1aa89a"
$colAmber     = ToWordColor "f2a541"
$colText      = ToWordColor "1c2b2b"
$colMuted     = ToWordColor "5b7877"

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
# Word's "AutoFormat As You Type" was silently converting the typed date
# and student-ID text (pattern-matched as a path/date) into auto-hyperlinks,
# overriding their explicit font colours with the theme's hyperlink blue.
$word.Options.AutoFormatAsYouTypeReplaceHyperlinks = $false

try {
    # ---- 1. Cover sheet ----
    $cover = $word.Documents.Open($coverPath, [ref]$false, [ref]$true)
    $cover.Content.Copy()
    $cover.Close([ref]$false)

    $report = $word.Documents.Open($reportPath, [ref]$false, [ref]$false)
    $sel = $report.ActiveWindow.Selection
    $sel.HomeKey($wdStory) | Out-Null
    $sel.PasteAndFormat($wdFormatOriginalFormatting) | Out-Null
    $sel.Collapse($wdCollapseEnd) | Out-Null
    $sel.InsertBreak($wdSectionBreakNextPage) | Out-Null

    $coverSection = $report.Sections.Item(1)
    $coverSection.PageSetup.TopMargin = 72
    $coverSection.PageSetup.BottomMargin = 72
    $coverSection.PageSetup.LeftMargin = 72
    $coverSection.PageSetup.RightMargin = 72
    $coverSection.PageSetup.PageWidth = 612   # 8.5in
    $coverSection.PageSetup.PageHeight = 792  # 11in

    Write-Host "Merged cover sheet ($($report.Sections.Count) sections)"

    # ---- 2. Title / cover page ----
    # $sel is now at the very start of Section 2, which is still, at the
    # paragraph-style level, the ORIGINAL "Heading 1" paragraph that used
    # to be the report's own title (only its direct character formatting
    # was pushed down by typing before it - the paragraph STYLE, including
    # Heading 1's bottom border, is still inherited by every new paragraph
    # mark created here until explicitly overridden). That inherited border
    # is what caused a stray line to show up on whichever title-page
    # paragraph happened to end a page. Resetting to "Normal" first fixes
    # it at the source, rather than fighting individual symptoms of it.
    $sel.Style = $report.Styles.Item("Normal")
    $sel.Font.Name = "Times New Roman"
    $sel.Font.Color = 0
    $sel.ParagraphFormat.Alignment = $wdAlignParagraphCenter

    $sel.TypeParagraph() | Out-Null
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Size = 13
    $sel.Font.Bold = 1
    $sel.Font.Color = $colAmber
    $sel.TypeText("International College of Business and Technology (ICBT) - Jaffna") | Out-Null
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Size = 12
    $sel.Font.Bold = 0
    $sel.Font.Italic = 1
    $sel.Font.Color = $colMuted
    $sel.TypeText("Cardiff Metropolitan University") | Out-Null
    $sel.Font.Italic = 0
    $sel.TypeParagraph() | Out-Null
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Size = 30
    $sel.Font.Bold = 1
    $sel.Font.Color = $colNavyDark
    $sel.TypeText("Sunrise Dental Clinic System") | Out-Null
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Size = 16
    $sel.Font.Color = $colTeal
    $sel.TypeText("Online Appointment & Patient Management System") | Out-Null
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Size = 12.5
    $sel.Font.Bold = 0
    $sel.Font.Italic = 1
    $sel.Font.Color = $colMuted
    $sel.TypeText("Assignment Report - CIS6003 Advanced Programming (WRIT1)") | Out-Null
    $sel.Font.Italic = 0
    $sel.TypeParagraph() | Out-Null
    $sel.TypeParagraph() | Out-Null

    # A decorative accent bar (matching the PDF's gradient bar) was tried
    # here via a paragraph bottom-border, but Word intermittently re-drew
    # a stray copy of that border on the following page after later page
    # breaks - a Word rendering quirk, not worth chasing for a purely
    # decorative element on the one output (DOCX) whose job is being a
    # correct, editable document rather than a pixel-exact match to the
    # PDF's own design. Simple spacing instead.
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Size = 11.5
    $sel.Font.Italic = 1
    $sel.Font.Color = $colText
    $sel.ParagraphFormat.Alignment = $wdAlignParagraphLeft
    $sel.TypeText("A full-stack, three-tier web application built to replace Sunrise Dental Clinic's paper-based booking process: a Spring Boot REST API secured with JWT authentication, a MySQL database with triggers/a stored procedure/views, five GoF design patterns, and a static HTML/CSS/JS client - covering appointment booking, patient records, dentist scheduling, and automated billing end-to-end.") | Out-Null
    $sel.Font.Italic = 0
    $sel.TypeParagraph() | Out-Null
    $sel.TypeParagraph() | Out-Null

    # Word silently re-colours certain typed text (dates, slash-separated
    # IDs) with its default hyperlink/smart-tag blue, overriding direct
    # Font.Color set beforehand - disabling AutoFormatAsYouTypeReplaceHyperlinks
    # (above) did not stop it. The reliable fix is to go back over the just-
    # typed range AFTER typing and force both the character style (clearing
    # any auto-applied "Hyperlink" style) and the colour again.
    function Restyle([object]$startPos, [double]$color, [bool]$bold = $false) {
        $endPos = $sel.Range.Start
        $r = $report.Range($startPos, $endPos)
        # If Word auto-inserted an actual Hyperlink field over this text,
        # remove the field (keeps the visible text, drops the field/style).
        for ($i = $r.Hyperlinks.Count; $i -ge 1; $i--) { $r.Hyperlinks.Item($i).Delete() | Out-Null }
        $r.Style = $report.Styles.Item("Default Paragraph Font")
        $r.Font.Color = $color
        $r.Font.Bold = [int]$bold
    }

    $sel.Font.Size = 12.5
    $sel.ParagraphFormat.Alignment = $wdAlignParagraphCenter
    $dateStart = $sel.Range.Start
    $sel.TypeText("September 5, 2026") | Out-Null
    Restyle $dateStart $colText
    $sel.TypeParagraph() | Out-Null
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Size = 12
    $sel.Font.Color = $colText
    $sel.ParagraphFormat.Alignment = $wdAlignParagraphLeft
    $sel.Font.Bold = 1
    $lblStart = $sel.Range.Start
    $sel.TypeText("Student Name: ") | Out-Null
    Restyle $lblStart $colText $true
    $sel.Font.Bold = 0
    $valStart = $sel.Range.Start
    $sel.TypeText("Kirisha") | Out-Null
    Restyle $valStart $colText
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Bold = 1
    $lblStart = $sel.Range.Start
    $sel.TypeText("Student ID: ") | Out-Null
    Restyle $lblStart $colText $true
    $sel.Font.Bold = 0
    $valStart = $sel.Range.Start
    $sel.TypeText("JF/BSCSD/19/33") | Out-Null
    Restyle $valStart $colText
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Bold = 1
    $lblStart = $sel.Range.Start
    $sel.TypeText("Course: ") | Out-Null
    Restyle $lblStart $colText $true
    $sel.Font.Bold = 0
    $valStart = $sel.Range.Start
    $sel.TypeText("BSc SE (Top-Up)") | Out-Null
    Restyle $valStart $colAmber
    $sel.TypeParagraph() | Out-Null

    # Reset formatting back to normal defaults before the page break, so
    # nothing bleeds into the TOC/report content that follows.
    $sel.Font.Bold = 0
    $sel.Font.Italic = 0
    $sel.Font.Size = 12
    $sel.Font.Color = 0  # wdColorAutomatic
    $sel.ParagraphFormat.Alignment = $wdAlignParagraphLeft
    $sel.InsertBreak($wdPageBreak) | Out-Null

    Write-Host "Inserted title/cover page"

    # ---- 3. Table of Contents ----
    $sel.Style = $report.Styles.Item("Title")
    $sel.TypeText("Table of Contents") | Out-Null
    $sel.TypeParagraph() | Out-Null
    $sel.Style = $report.Styles.Item("Normal")

    $tocRange = $sel.Range.Duplicate
    $tocRange.Collapse($wdCollapseStart)
    $toc = $report.TablesOfContents.Add($tocRange, $true, 2, 4, $true, [Type]::Missing, $true, $true, [Type]::Missing, $true)

    $afterToc = $toc.Range.Duplicate
    $afterToc.Collapse($wdCollapseEnd)
    $afterToc.InsertBreak($wdPageBreak) | Out-Null

    Write-Host "Inserted Table of Contents (Heading 2-4, hyperlinked)"

    # ---- 4. Save ----
    $report.Fields.Update() | Out-Null
    $report.Save()
    Write-Host "Saved $reportPath"

    $report.Close([ref]$false)
} finally {
    $word.Quit()
}

Write-Host "DONE. ASSIGNMENT_REPORT.docx is now final and submission-ready. Run 'node generate-pdf.js' separately for the matching PDF."
