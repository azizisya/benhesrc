// $ANTLR 2.7.4: "terrier.g" -> "TerrierQueryParser.java"$

package uk.ac.gla.terrier.querying.parser;
import antlr.TokenStreamSelector;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

public class TerrierQueryParser extends antlr.LLkParser       implements TerrierQueryParserTokenTypes
 {

	private TokenStreamSelector selector;
	public void setSelector(TokenStreamSelector s)
	{
		selector = s;
	}

protected TerrierQueryParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public TerrierQueryParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected TerrierQueryParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public TerrierQueryParser(TokenStream lexer) {
  this(lexer,2);
}

public TerrierQueryParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final Query  query() throws RecognitionException, TokenStreamException {
		Query q;
		
		q=null;
		
		try {      // for error handling
			if ((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2)))) {
				/*System.out.print("implied ");*/
				q=impliedMultiTermQuery();
			}
			else if ((LA(1)==OPEN_PAREN) && (_tokenSet_2.member(LA(2)))) {
				/*System.out.print("explicit ");*/
				q=explicitMultiTermQuery();
			}
			else if ((LA(1)==QUOTE) && (LA(2)==ALPHANUMERIC)) {
				q=phraseQuery();
			}
			else if ((LA(1)==ALPHANUMERIC) && (LA(2)==COLON)) {
				q=fieldQuery();
			}
			else if ((LA(1)==REQUIRED||LA(1)==NOT_REQUIRED) && (LA(2)==ALPHANUMERIC||LA(2)==QUOTE||LA(2)==OPEN_PAREN)) {
				q=requirementQuery();
			}
			else if ((LA(1)==ALPHANUMERIC) && (LA(2)==EOF||LA(2)==HAT)) {
				q=singleTermQuery();
			}
			else if ((LA(1)==ALPHANUMERIC) && (LA(2)==OPEN_SQUARE_PAREN)) {
				q=functionQuery();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_3);
		}
		return q;
	}
	
	public final Query  impliedMultiTermQuery() throws RecognitionException, TokenStreamException {
		Query q;
		
		q = null;
		MultiTermQuery mtq = new MultiTermQuery();
		Query child = null;
		
		try {      // for error handling
			{
			int _cnt30=0;
			_loop30:
			do {
				if ((_tokenSet_0.member(LA(1)))) {
					{
					switch ( LA(1)) {
					case REQUIRED:
					case NOT_REQUIRED:
					{
						child=requirementQuery();
						break;
					}
					case QUOTE:
					{
						child=phraseQuery();
						break;
					}
					case OPEN_PAREN:
					{
						/*System.out.print("explicit ");*/
						child=explicitMultiTermQuery();
						break;
					}
					default:
						if ((LA(1)==ALPHANUMERIC) && (LA(2)==OPEN_SQUARE_PAREN)) {
							child=functionQuery();
						}
						else if ((LA(1)==ALPHANUMERIC) && (_tokenSet_4.member(LA(2)))) {
							child=singleTermQuery();
						}
						else if ((LA(1)==ALPHANUMERIC) && (LA(2)==COLON)) {
							child=fieldQuery();
						}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					if (child != null) mtq.add(child);
				}
				else {
					if ( _cnt30>=1 ) { break _loop30; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt30++;
			} while (true);
			}
			q = mtq;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_3);
		}
		return q;
	}
	
	public final Query  explicitMultiTermQuery() throws RecognitionException, TokenStreamException {
		Query q;
		
		Token  closeP = null;
		q= null; MultiTermQuery mtq = new MultiTermQuery(); Query child = null;
		
		try {      // for error handling
			match(OPEN_PAREN);
			{
			int _cnt34=0;
			_loop34:
			do {
				if ((_tokenSet_2.member(LA(1))) && (_tokenSet_5.member(LA(2)))) {
					{
					switch ( LA(1)) {
					case REQUIRED:
					case NOT_REQUIRED:
					{
						child=requirementQuery();
						break;
					}
					case QUOTE:
					{
						child=phraseQuery();
						break;
					}
					default:
						if ((LA(1)==ALPHANUMERIC) && (LA(2)==OPEN_SQUARE_PAREN)) {
							child=functionQuery();
						}
						else if ((LA(1)==ALPHANUMERIC) && (_tokenSet_6.member(LA(2)))) {
							child=singleTermQuery();
						}
						else if ((LA(1)==ALPHANUMERIC) && (LA(2)==COLON)) {
							child=fieldQuery();
						}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					mtq.add(child);
				}
				else {
					if ( _cnt34>=1 ) { break _loop34; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt34++;
			} while (true);
			}
			{
			if ((LA(1)==CLOSE_PAREN) && (_tokenSet_7.member(LA(2)))) {
				closeP = LT(1);
				match(CLOSE_PAREN);
			}
			else if ((_tokenSet_7.member(LA(1))) && (_tokenSet_5.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			if (closeP == null ){ /* WARN missing '(' */ }
			q= mtq;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return q;
	}
	
	public final Query  phraseQuery() throws RecognitionException, TokenStreamException {
		Query q;
		
		Token  endQ = null;
		Token  p = null;
		int prox = 0; 
			PhraseQuery pq = new PhraseQuery(); MultiTermQuery mq = new MultiTermQuery();
			Query child = null; q = mq;
		
		try {      // for error handling
			match(QUOTE);
			{
			int _cnt12=0;
			_loop12:
			do {
				if ((LA(1)==ALPHANUMERIC) && (_tokenSet_6.member(LA(2)))) {
					child=singleTermQuery();
					pq.add(child); mq.add(child);
				}
				else {
					if ( _cnt12>=1 ) { break _loop12; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt12++;
			} while (true);
			}
			{
			if ((LA(1)==QUOTE) && (_tokenSet_8.member(LA(2)))) {
				endQ = LT(1);
				match(QUOTE);
				{
				switch ( LA(1)) {
				case PROXIMITY:
				{
					match(PROXIMITY);
					selector.push("numbers");System.out.println("Changed");
					p = LT(1);
					match(NUM_INT);
					prox = Integer.parseInt(p.getText()); selector.pop();System.out.println("Changed back");
					break;
				}
				case EOF:
				case ALPHANUMERIC:
				case QUOTE:
				case REQUIRED:
				case NOT_REQUIRED:
				case OPEN_PAREN:
				case CLOSE_PAREN:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
			}
			else if ((_tokenSet_7.member(LA(1))) && (_tokenSet_5.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			
						/* phrase has been closed, use phraseQuery, not multiTermQuery*/
						if (endQ != null) {q = pq;}
						else{/*WARN quote was emitted */}
					
			if (prox>0) pq.setProximityDistance(prox);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return q;
	}
	
	public final Query  fieldQuery() throws RecognitionException, TokenStreamException {
		Query q;
		
		Token  f = null;
		q= null; FieldQuery fq = new FieldQuery(); Query child = null;
		
		try {      // for error handling
			f = LT(1);
			match(ALPHANUMERIC);
			fq.setField(f.getText());
			match(COLON);
			{
			switch ( LA(1)) {
			case ALPHANUMERIC:
			{
				child=singleTermQuery();
				break;
			}
			case QUOTE:
			{
				child=phraseQuery();
				break;
			}
			case OPEN_PAREN:
			{
				child=explicitMultiTermQuery();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			fq.setChild(child);
			q = fq;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return q;
	}
	
	public final Query  requirementQuery() throws RecognitionException, TokenStreamException {
		Query q;
		
		q= null; RequirementQuery rq = new RequirementQuery(); Query child = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case REQUIRED:
			{
				match(REQUIRED);
				break;
			}
			case NOT_REQUIRED:
			{
				match(NOT_REQUIRED);
				rq.setRequired(false);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case QUOTE:
			{
				child=phraseQuery();
				break;
			}
			case OPEN_PAREN:
			{
				child=explicitMultiTermQuery();
				break;
			}
			default:
				if ((LA(1)==ALPHANUMERIC) && (_tokenSet_6.member(LA(2)))) {
					child=singleTermQuery();
				}
				else if ((LA(1)==ALPHANUMERIC) && (LA(2)==COLON)) {
					child=fieldQuery();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			rq.setChild(child);
			q = rq;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return q;
	}
	
	public final Query  singleTermQuery() throws RecognitionException, TokenStreamException {
		Query q;
		
		Token  qt = null;
		Token  w_f = null;
		Token  w_nf = null;
		Token  w_i = null;
		Token  w_ni = null;
		q= null; SingleTermQuery stq = new SingleTermQuery();
		
		try {      // for error handling
			qt = LT(1);
			match(ALPHANUMERIC);
			stq.setTerm(qt.getText());
			{
			switch ( LA(1)) {
			case HAT:
			{
				match(HAT);
				selector.push("numbers");
				{
				switch ( LA(1)) {
				case NUM_FLOAT:
				{
					w_f = LT(1);
					match(NUM_FLOAT);
					stq.setWeight(Double.parseDouble(w_f.getText())); selector.pop();
					break;
				}
				case NEG_NUM_FLOAT:
				{
					w_nf = LT(1);
					match(NEG_NUM_FLOAT);
					stq.setWeight(Double.parseDouble(w_nf.getText())); selector.pop();
					break;
				}
				case NUM_INT:
				{
					w_i = LT(1);
					match(NUM_INT);
					stq.setWeight(Double.parseDouble(w_i.getText())); selector.pop();
					break;
				}
				case NEG_NUM_INT:
				{
					w_ni = LT(1);
					match(NEG_NUM_INT);
					stq.setWeight(Double.parseDouble(w_ni.getText())); selector.pop();
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case EOF:
			case ALPHANUMERIC:
			case QUOTE:
			case REQUIRED:
			case NOT_REQUIRED:
			case OPEN_PAREN:
			case CLOSE_PAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			q = stq;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return q;
	}
	
	public final Query  functionQuery() throws RecognitionException, TokenStreamException {
		Query q;
		
		Token  fn = null;
		Token  cPar = null;
		Token  fWeight = null;
		Token  iWeight = null;
		q = null;
		FunctionQuery fq = new FunctionQuery();
		Query child = null;
		
		try {      // for error handling
			fn = LT(1);
			match(ALPHANUMERIC);
			fq.setFName(fn.getText());
			match(OPEN_SQUARE_PAREN);
			{
			int _cnt17=0;
			_loop17:
			do {
				if ((LA(1)==OPEN_PAREN)) {
					/*System.out.print("argument ");*/
					child=argumentMultiTermQuery();
					fq.add(child);
				}
				else {
					if ( _cnt17>=1 ) { break _loop17; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt17++;
			} while (true);
			}
			{
			cPar = LT(1);
			match(CLOSE_SQUARE_PAREN);
			{
			switch ( LA(1)) {
			case HAT:
			{
				match(HAT);
				selector.push("numbers");
				{
				switch ( LA(1)) {
				case NUM_FLOAT:
				{
					fWeight = LT(1);
					match(NUM_FLOAT);
					fq.setWeight(Double.parseDouble(fWeight.getText())); selector.pop();
					break;
				}
				case NUM_INT:
				{
					iWeight = LT(1);
					match(NUM_INT);
					fq.setWeight(Double.parseDouble(iWeight.getText())); selector.pop();
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case EOF:
			case ALPHANUMERIC:
			case QUOTE:
			case REQUIRED:
			case NOT_REQUIRED:
			case OPEN_PAREN:
			case CLOSE_PAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			}
			if (cPar != null) { q = fq; }
			else { System.out.println("terrier.g:functionQuery missing closing parenthesis"); }
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return q;
	}
	
	public final Query  argumentMultiTermQuery() throws RecognitionException, TokenStreamException {
		Query q;
		
		Token  closeP = null;
		Token  fWeight = null;
		Token  iWeight = null;
		q= null; MultiTermQuery mtq = new MultiTermQuery(); Query child = null;
		
		try {      // for error handling
			match(OPEN_PAREN);
			{
			int _cnt23=0;
			_loop23:
			do {
				if ((LA(1)==ALPHANUMERIC)) {
					child=singleTermQuery();
					mtq.add(child);
				}
				else {
					if ( _cnt23>=1 ) { break _loop23; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt23++;
			} while (true);
			}
			{
			closeP = LT(1);
			match(CLOSE_PAREN);
			{
			switch ( LA(1)) {
			case HAT:
			{
				match(HAT);
				selector.push("numbers");
				{
				switch ( LA(1)) {
				case NUM_FLOAT:
				{
					fWeight = LT(1);
					match(NUM_FLOAT);
					mtq.setWeight(Double.parseDouble(fWeight.getText())); selector.pop();
					break;
				}
				case NUM_INT:
				{
					iWeight = LT(1);
					match(NUM_INT);
					mtq.setWeight(Double.parseDouble(iWeight.getText())); selector.pop();
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case OPEN_PAREN:
			case CLOSE_SQUARE_PAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			}
			if (closeP != null) { q = mtq; }
			else { System.out.println("terrier.g:explicitMultiTermQuery missing closing parenthesis"); }
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_9);
		}
		return q;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"NUM_FLOAT",
		"NEG_NUM_FLOAT",
		"PERIOD",
		"HYPHEN",
		"DIT",
		"INT",
		"NUM_INT",
		"NEG_NUM_INT",
		"ALPHANUMERIC_CHAR",
		"ALPHANUMERIC",
		"COLON",
		"HAT",
		"QUOTE",
		"REQUIRED",
		"NOT_REQUIRED",
		"OPEN_PAREN",
		"CLOSE_PAREN",
		"OPEN_SQUARE_PAREN",
		"CLOSE_SQUARE_PAREN",
		"PROXIMITY",
		"IGNORED"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 991232L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 3137538L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 466944L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 1024002L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 4186114L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 2072578L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 2039810L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 10428418L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 4718592L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	
	}
