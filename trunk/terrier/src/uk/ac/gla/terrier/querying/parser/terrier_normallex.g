header {
package uk.ac.gla.terrier.querying.parser;
}


// ----------------------------------------------------------------------------
// the main lexer

class TerrierLexer extends Lexer;

options 
{
    k = 1;
	/* we need to set this for  antlr < 2.7.5, so the set complement
	 * functions correctly. */
    // Allow any char but \uFFFF (16 bit -1)
    charVocabulary='\u0003'..'\uFFFE';
//	charVocabulary = '\3'..'\377';
	exportVocab=Main;
	importVocab=Numbers;
	defaultErrorHandler=false;
}

protected
ALPHANUMERIC_CHAR:	'0'..'9'|'a'..'z'|'A'..'Z'|'\200'..'\uFFFE';

ALPHANUMERIC:   (ALPHANUMERIC_CHAR)+;

//used for fields and boosting weights
COLON:        ':'
     ;

//before weights
HAT:          '^'
   ;

//start and end of a phrase
QUOTE:        '\"'
     ;

//required token
REQUIRED:     '+'
        ;

//not required token
NOT_REQUIRED: '-';

//opening parenthesis
OPEN_PAREN: '(';

//closing parenthesis
CLOSE_PAREN: ')';

//proximity operator
PROXIMITY: '~';


IGNORED:
    (~(
		':'|'^'|'\"'|'-'|'+'|'('|')'|'~'|'.'|
		'A'..'Z'|'a'..'z'|'0'..'9'|'\200'..'\uFFFE'))
   { $setType(Token.SKIP); }
;
