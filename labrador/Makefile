

#DEFINITIONS
HTML_CSS=/docs/pod/bits/pod.css
HTML_CSS=/~macdonch/labrador/pod.css
HTML_TARGET=/content/www/docs/labrador/
HTML_TARGET=${HOME}/public_html/labrador/pod/

#TARGETS

test:
	find lib/ | grep \.pm$ | lib/check.pl
	find bin/ | grep \.pl$ | lib/check.pl

testdocs:
	find lib/ | grep \.pm$ | lib/check.pl -p
	find doc/ | grep \.pod$ | lib/check.pl -p

pod2html:
	pods2html -css ${HTML_CSS} --index Labrador --toc lib/ ${HTML_TARGET}

