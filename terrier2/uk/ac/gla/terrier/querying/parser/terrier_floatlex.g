header {
package uk.ac.gla.terrier.querying.parser;
}


class TerrierFloatLexer extends Lexer;
options {
	exportVocab=Numbers;
}

tokens{
	NUM_FLOAT;
	NEG_NUM_FLOAT;
}

protected
PERIOD:	'.'	;

protected
HYPHEN: '-' ;

protected
DIT:	'0'..'9';

protected
INT:	(DIT)+	;

//a query term
NUM_INT:	INT ('.' INT { _ttype = NUM_FLOAT; } )?;
NEG_NUM_INT:	HYPHEN INT ('.' INT { _ttype = NEG_NUM_FLOAT; } )?;
