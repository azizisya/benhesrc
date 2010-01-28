all: Report.ps

all_images:
	${MAKE} -C images

2up: Report-2up.ps

xpdf: Report.pdf
	xpdf Report.pdf

ggv: Report.ps
	ggv Report.ps

gv: Report.ps
	gv Report.ps

web: Report.pdf
	cp Report.pdf ${HOME}/public_html/
	chmod 644 ${HOME}/public_html/Report.pdf

REPORT_TARGETS=biblio.bib biblio.bst section_abstract.tex chapter_introduction.tex chapter_background.tex chapter_architecture.tex chapter_politeness.tex chapter_integration.tex chapter_futurework.tex

Report.dvi: Report.tex ${REPORT_TARGETS} all_images
	latex Report.tex
	bibtex Report
	latex Report.tex
	latex Report.tex

Report-2up.ps: Report.ps
	psnup -2 Report.ps > Report-2up.ps	

Report.ps: Report.dvi
	dvips -o Report.ps Report.dvi

pdf: all_images Report.pdf

Report.pdf: Report.ps
	ps2pdf Report.ps

help:
	echo "Targets available are all, pdf, clean, help"

clean:
	rm -f Report.dvi Report.ps Report.pdf Report.toc *.log *.aux
	${MAKE} -C images clean
