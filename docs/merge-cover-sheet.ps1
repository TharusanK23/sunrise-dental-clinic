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
    # Uses Range.FormattedText to transfer the cover sheet's content (with
    # its original formatting) directly between the two open documents,
    # instead of Copy()/PasteAndFormat() via the system clipboard - the
    # clipboard proved unreliable (locked/unavailable) in some environments,
    # and FormattedText avoids it entirely while producing the same result.
    $cover = $word.Documents.Open($coverPath, [ref]$false, [ref]$true)
    $coverFormatted = $cover.Content.FormattedText

    $report = $word.Documents.Open($reportPath, [ref]$false, [ref]$false)
    $insertRange = $report.Range(0, 0)
    $insertRange.FormattedText = $coverFormatted
    $cover.Close([ref]$false)

    $sel = $report.ActiveWindow.Selection
    $sel.SetRange($insertRange.Start, $insertRange.End) | Out-Null
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
    # $sel is now at the very start of Section 2, still collapsed INSIDE
    # the original "Heading 1" paragraph that is the report's own title
    # ("CIS6003 Advanced Programming - Assignment Report") - no new
    # paragraph mark exists yet at this point. Setting Selection.Style on a
    # collapsed selection sets the style of the paragraph CONTAINING it, so
    # doing that here directly demotes the report's own title from Heading
    # 1 to Normal (found by rendering and comparing - the title silently
    # lost its bold/border and became plain body text). InsertParagraphBefore()
    # first creates a genuinely separate, empty paragraph ahead of the
    # original one, so the style reset below applies to that new paragraph
    # only, leaving the report's real title paragraph (now pushed further
    # down, after all the title-page content) completely untouched.
    $titleAnchor = $sel.Range.Start
    $report.Range($titleAnchor, $titleAnchor).InsertParagraphBefore() | Out-Null
    $sel.SetRange($titleAnchor, $titleAnchor) | Out-Null
    $sel.Style = $report.Styles.Item("Normal")
    $sel.Font.Name = "Times New Roman"
    $sel.Font.Color = 0
    $sel.ParagraphFormat.Alignment = $wdAlignParagraphCenter

    $sel.TypeParagraph() | Out-Null
    $sel.TypeParagraph() | Out-Null

    # Word silently re-colours/de-italicises certain typed text (dates,
    # slash-separated IDs, and - as found here - the trailing word of a
    # recognised institution name like "...University") after the fact,
    # overriding direct Font.Color/Font.Italic set beforehand - disabling
    # AutoFormatAsYouTypeReplaceHyperlinks (above) does not stop it. The
    # reliable fix is to go back over the just-typed range AFTER typing and
    # force the character style (clearing any auto-applied style/field) and
    # the intended formatting again. Defined here so it can be used for the
    # university/affiliation lines below, not just the date/name/ID/course
    # block further down.
    function Restyle([object]$startPos, [double]$color, [bool]$bold = $false, [bool]$italic = $false) {
        $endPos = $sel.Range.Start
        $r = $report.Range($startPos, $endPos)
        # If Word auto-inserted an actual Hyperlink field over this text,
        # remove the field (keeps the visible text, drops the field/style).
        for ($i = $r.Hyperlinks.Count; $i -ge 1; $i--) { $r.Hyperlinks.Item($i).Delete() | Out-Null }
        $r.Style = $report.Styles.Item("Default Paragraph Font")
        $r.Font.Color = $color
        $r.Font.Bold = [int]$bold
        $r.Font.Italic = [int]$italic
    }

    $sel.Font.Size = 13
    $sel.Font.Bold = 1
    $sel.Font.Color = $colAmber
    $icbtStart = $sel.Range.Start
    $sel.TypeText("International College of Business and Technology (ICBT) - Jaffna") | Out-Null
    Restyle $icbtStart $colAmber $true
    $sel.TypeParagraph() | Out-Null

    # Root cause of the "University" word losing its italic (found by
    # bisecting with a minimal repro): setting Selection.Font.Italic on a
    # COLLAPSED selection sitting exactly at the end of a just-inserted run
    # retroactively splits off and de-formats that run's last word - not an
    # AutoCorrect/AutoFormat effect at all. Range.InsertAfter() (rather than
    # Selection.TypeText) is still used here to build the text without
    # simulating keystrokes, but the actual fix is ordering: move the
    # selection past a paragraph break BEFORE changing its Font, never while
    # still collapsed at the boundary of the text just inserted.
    $affRange = $report.Range($sel.Range.Start, $sel.Range.Start)
    $affRange.InsertAfter("Cardiff Metropolitan University") | Out-Null
    $affRange.Font.Size = 12
    $affRange.Font.Bold = 0
    $affRange.Font.Italic = 1
    $affRange.Font.Color = $colMuted
    $sel.SetRange($affRange.End, $affRange.End) | Out-Null
    $sel.TypeParagraph() | Out-Null
    $sel.Font.Italic = 0
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
    # Font changes on a collapsed selection must happen AFTER moving past a
    # paragraph break, never while still collapsed at the end of the text
    # just typed - see the note above the affiliation-line block for why
    # (it silently de-italicises that text's last word otherwise).
    $sel.TypeParagraph() | Out-Null
    $sel.Font.Italic = 0
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
    $sel.TypeParagraph() | Out-Null
    $sel.Font.Italic = 0
    $sel.TypeParagraph() | Out-Null

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
    # Note: no "$sel.Font.Bold = 0" between the label and value TypeText
    # calls below (unlike an earlier version of this script) - that's the
    # same collapsed-selection-adjacent-to-just-typed-text pattern that
    # corrupted the affiliation line (see the note above it), and here it
    # would corrupt the tail of the LABEL instead. Restyle() already forces
    # the value's bold=false correctly via Range afterward, so the
    # Selection-level reset isn't needed.
    $sel.Font.Bold = 1
    $lblStart = $sel.Range.Start
    $sel.TypeText("Student Name: ") | Out-Null
    Restyle $lblStart $colText $true
    $valStart = $sel.Range.Start
    $sel.TypeText("Kirisha") | Out-Null
    Restyle $valStart $colText
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Bold = 1
    $lblStart = $sel.Range.Start
    $sel.TypeText("Student ID: ") | Out-Null
    Restyle $lblStart $colText $true
    $valStart = $sel.Range.Start
    $sel.TypeText("JF/BSCSD/19/33") | Out-Null
    Restyle $valStart $colText
    $sel.TypeParagraph() | Out-Null

    $sel.Font.Bold = 1
    $lblStart = $sel.Range.Start
    $sel.TypeText("Course: ") | Out-Null
    Restyle $lblStart $colText $true
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
