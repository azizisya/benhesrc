
FIG2DEV=fig2dev
FIG2PS= -L eps

all: crest.ps disp_layers.eps disp_layers2.eps slicedqueue.eps frontend.ps crawler_overview.eps crawler_layers.eps delayline_hosts.eps graph_pl2c.ps

crawler_layers.eps: crawler_layers.fig
	${FIG2DEV} ${FIG2PS} crawler_layers.fig crawler_layers.eps

crawler_overview.eps: crawler_overview.fig
	${FIG2DEV} ${FIG2PS} crawler_overview.fig crawler_overview.eps

delayline_hosts.eps: delayline_hosts.fig
	${FIG2DEV} ${FIG2PS} delayline_hosts.fig delayline_hosts.eps

disp_layers.eps: disp_layers.fig
	${FIG2DEV} ${FIG2PS} disp_layers.fig disp_layers.eps

disp_layers2.eps: disp_layers2.fig
	${FIG2DEV} ${FIG2PS} disp_layers2.fig disp_layers2.eps

frontend.ps : frontend.gif
	giftopnm frontend.gif > frontend.pnm
	pnmtops -noturn frontend.pnm > frontend.ps
	rm frontend.pnm

graph_pl2c.ps: graph_pl2c.gif
	giftopnm graph_pl2c.gif > graph_pl2c.pnm
	pnmtops -noturn graph_pl2c.pnm > graph_pl2c.ps
	rm graph_pl2c.pnm

slicedqueue.eps: slicedqueue.fig
	${FIG2DEV} ${FIG2PS} slicedqueue.fig slicedqueue.eps	

clean:
	rm -f slicedqueue.eps disp_layers.eps *.bak

