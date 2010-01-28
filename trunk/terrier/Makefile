
-include config

do: clean compile

compile: lib/${JAR_NAME}.jar doc/javadoc/
	mkdir -p ${DIST_DIRS}

#build the configuration file. This should be updated if either the Makefile or the configure script has been updated
config: Makefile bin/configure.sh share/MANIFEST.txt
	bin/configure.sh


#build the jar file
lib/${JAR_NAME}.jar: config ${JAVA_FILES}
	cd src/uk/ac/gla/terrier/querying/parser &>/dev/null && \
		${JAVA_HOME}/bin/java -cp ${CLASSPATH} antlr.Tool terrier_floatlex.g &&\
		${JAVA_HOME}/bin/java -cp ${CLASSPATH} antlr.Tool terrier_normallex.g && \
		${JAVA_HOME}/bin/java -cp ${CLASSPATH} antlr.Tool terrier.g

	mkdir -p ${TMPDIR}
	${JAVA_HOME}/bin/javac \
		-classpath ${CLASSPATH}:src/ \
		-d ${TMPDIR} ${JAVA_FILES}
	rm -f lib/${JAR_NAME}.jar
	${JAVA_HOME}/bin/jar cfM lib/${JAR_NAME}.jar \
		-C ${TMPDIR} .
	ls lib/${JAR_NAME}.jar
	rm -rf ${TMPDIR}

#build the javadoc only
docs: doc/javadoc/

#update the javadoc if the jar file has been updated
doc/javadoc/: lib/${JAR_NAME}.jar
	mkdir -p doc/javadoc/
	${JAVA_HOME}/bin/javadoc -J-Xms120m -J-Xmx120m -overview src/overview.html -splitIndex \
		-windowtitle 'Terrier Information Retrieval Platform version ${VERSION} API Specification' \
		-header '<b>Terrier IR Platform</b><br><font size="-1">${VERSION}</font>' \
		-footer '<b>Terrier IR Platform</b><br><font size="-1">${VERSION}</font>' \
		-doctitle 'Terrier Information Retrieval Platform version ${VERSION} API Specification' \
		-bottom '<font size="-1"><a href="http://ir.dcs.gla.ac.uk/terrier/">Terrier Information Retrieval Platform</a> ${VERSION}. Copyright 2004-2008 <a href="http://www.gla.ac.uk/">University of Glasgow</a></font>' \
	-d doc/javadoc \
	-public -use -author -version -classpath ${CLASSPATH}:lib/${JAR_NAME}.jar ${JAVA_FILES}

#remove rubbish
clean:
	rm -rf ${TAR_NAME}.* ${DIST} doc/javadoc/ config lib/${JAR_NAME}.jar
	find src/ -type f -name "*.class" | xargs rm -f

#make the desired default distribution
zip: ${DIST}
distribution: zip

#make the distribution for the unix platform
unix: ${TAR_NAME}.tar.${UNIX_COMPRESSION}
	@echo
	@echo "Distribution can be found as ${TAR_NAME}.tar.${UNIX_COMPRESSION}"

#make the distribution for thw windows platform
win: ${TAR_NAME}.zip
	@echo
	@echo "Distribution can be found as ${TAR_NAME}.zip"

#build the distribution dir - copy over ALL (and only) the files in the MANIFEST.txt
#making directories along the way
dist: compile
	rm -rf ${DIST}
	mkdir -p ${DIST}
	cd ${DIST}; mkdir -p ${DIST_DIRS}
	for LINE in `cat ${MANIFEST}`; do if [ -f $$LINE ]; then DIR=`dirname $$LINE`; mkdir -p ${DIST}/$$DIR; cp $$LINE ${DIST}/$$LINE; else echo "WARNING: file $$LINE is in MANIFEST.txt but is missing"; fi; done


#build the tar file, has to take a copy of the distribution folder to ensure compressesed directory name is correct
${TAR_NAME}.tar: dist
	rm -rf ${DIR_NAME}
	cp -r ${DIST} ${DIR_NAME}
	${TAR} -cvf ${TAR_NAME}.tar ${DIR_NAME}/
	rm -rf ${DIR_NAME}

#gzip the tar file
${TAR_NAME}.tar.gz: ${TAR_NAME}.tar
	rm -f ${TAR_NAME}.tar.gz
	${GZIP} ${TAR_NAME}.tar

#bzip the tar file
${TAR_NAME}.tar.bz2: ${TAR_NAME}.tar
	rm -f ${TAR_NAME}.tar.gz
	${BZIP} ${TAR_NAME}.tar

#building for windows platform: unix2dos all the line-endings; build .zip file
${TAR_NAME}.zip: dist
	cd ${DIST}; cat ${MANIFEST} | egrep "\.(java|txt)$$" | xargs unix2dos 
	ln -s ${DIST} ${DIR_NAME}
	${ZIP} -r ${TAR_NAME}.zip ${DIR_NAME}
	rm -f ${DIR_NAME}


test: ${TEST_SCRIPTS}
	sh -c ' echo ${TEST_SCRIPTS} | xargs -n 1 bash ' 2>&1 | tee test.log | egrep '^Test'
