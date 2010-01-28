/*
 * Created on 10 Apr 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.indexing;

import java.io.IOException;
import java.io.StringWriter;

public class HtmlTokenizer extends TRECFullTokenizer {
	
	/** An option to ignore missing closing tags. Used for the query files. */
	protected boolean ignoreMissingClosingTags = true;
	
	String text = null;
	
	int textCounter = 0;
	
	private int nextChar(){
		int ch = (textCounter!=text.length())?(text.charAt(textCounter++)):(-1);
		//System.out.print((char)ch);
		return ch;
	}
	
	/**
	 * nextTermWithNumbers gives the first next string which is not a tag. All
	 * encounterd tags are pushed or popped according they are initial or final
	 */
	public String nextToken() {
		//the string to return as a result at the end of this method.
		String s = null;
		StringWriter sw = null;
		//are we in a body of a tag?
		boolean btag = true;
		//stringBuffer.delete(0, stringBuffer.length());
		int ch = 0;
		//are we reading the document number?
		boolean docnumber = false;
		//while not the end of document, or the end of file, or we are in a tag
		while (btag && ch!=-1 & textCounter!=text.length()) {
			sw = new StringWriter();
			boolean tag_f = false;
			boolean tag_i = false;
			error = false;
			try {
				if (lastChar == 60)
					ch = lastChar;
				//else
					//ch = this.nextChar();
				//Modified by G.Amati 7 june 2002
				//Removed a check: ch!=62
				//If not EOF and ch.isNotALetter and ch.isNotADigit and
				// ch.isNot '<' and ch.isNot '&'
				while (ch != -1 && (ch < 'A' || ch > 'Z') && (ch < 'a' || ch > 'z')
						&& (ch < '0' || ch > '9') && ch != '<' && ch != '&') {
					ch = this.nextChar();
					counter++;
					//if ch is '>' (end of tag), then there is an error.
					if (ch == '>')
						error = true;
				}
				//if a tag begins
				if (ch == '<') {
					ch = this.nextChar();
					counter++;
					//if it is a closing tag, set tag_f true
					if (ch == '/') {
						ch = this.nextChar();
						counter++;
						tag_f = true;
					} else if (ch == '!') { //else if it is a comment, that is
										   // <!
						//read until you encounter a '<', or a '>', or the end
						// of file
						while ((ch = this.nextChar()) != '>' && ch != '<' && ch != -1) {
							counter++;
						}
						counter++;
					} else
						tag_i = true; //otherwise, it is an opening tag
				}
				//Modified by V.Plachouras to take into account the exact tags
				if (ch == '&' && !stk.empty()
						&& !exactTagSet.isTagToProcess((String) stk.peek())) {
					//Modified by G.Amati 7 june 2002 */
					//read until an opening or the end of a tag is encountered,
					// or the end of file, or a space, or a semicolon,
					//which means the end of the escape sequence &xxx;
					while ((ch = this.nextChar()) != '>' && ch != '<' && ch != -1
							&& ch != ' ' && ch != ';') {
						counter++;
					}
					counter++;
				}
				//ignore all the spaces encountered
				while (ch == ' ') {
					ch = this.nextChar();
					counter++;
				}
				//if the body of a tag is encountered
				if ((btag = (tag_f || tag_i))) {
					//read until the end of file, or the start, or the end of a
					// tag, and save the content of the tag
					while (ch != -1 && ch != '<' && ch != '>') {
						sw.write(ch);
						ch = this.nextChar();
						counter++;
					}
				} else { //otherwise, if we are not in the body of a tag
					if (!stk.empty())
						docnumber = inDocnoTag(); // //check if we are in a DOCNO tag
					//if we are in a DOCNO tag
					if (docnumber) {
						//read and save the characters until encountering a '<'
						// or a '>'
						while (ch != -1 && ch != '<' && ch != '>') {
							sw.write(ch);
							ch = this.nextChar();
							counter++;
						}
					}
					if (!stk.empty() && !exactTagSet.isTagToProcess((String) stk.peek())) {
						//read a sequence of letters or digits.
						while (ch != -1
								&& (((ch >= 'A') && (ch <= 'Z'))
								 || ((ch >= 'a') && (ch <= 'z')) 
								 || ((ch >= '0') && (ch <= '9')))) {
							//transforms the uppercase character to lowercase
							if ((ch >= 'A') && (ch <= 'Z') && lowercase)
								ch += 32;
							sw.write(ch);
							ch = this.nextChar();
							counter++;
						}
					} else {
						//read a sequence of letters or digits.
						while (ch != -1
							   && (//ch == '&' || 
							     ((ch >= 'A') && (ch <= 'Z'))
								|| ((ch >= 'a') && (ch <= 'z')) 
								|| ((ch >= '0') && (ch <= '9')))) {
							//transforms the uppercase character to lowercase
							if ((ch >= 'A') && (ch <= 'Z') && lowercase)
								ch += 32;
							sw.write(ch);
							ch = this.nextChar();
							counter++;
						}
						// ignore chars after &
						if (ch != -1 && ch == '&') {
							while ((ch != -1
							   && (ch == '&' || 
							     ((ch >= 'A') && (ch <= 'Z'))
								|| ((ch >= 'a') && (ch <= 'z')) 
								|| ((ch >= '0') && (ch <= '9'))))){
								ch = this.nextChar();
								counter++;
							}
						}
					}
				}
				lastChar = ch;
				s = sw.toString();
				sw.close();
				//System.out.println("--------------"+s);
				if (tag_i) {
					if ((tagSet.isTagToProcess(s) || tagSet.isTagToSkip(s))
							&& !s.equals("")) {
						stk.push(s);
						if (tagSet.isTagToProcess(s)) {
							inTagToProcess = true;
							inTagToSkip = false;
							if (tagSet.isIdTag(s))
								inDocnoTag = true;
							else
								inDocnoTag = false;
						} else {
							inTagToSkip = true;
							inTagToProcess = false;
						}
					}
					return null;
				}
				if (tag_f) {
					if ((tagSet.isTagToProcess(s) || tagSet.isTagToSkip(s))
							&& !s.equals("")) {
						processEndOfTag(s);
						String stackTop = null;
						if (!stk.isEmpty()) {
							stackTop = (String) stk.peek();
							if (tagSet.isTagToProcess(stackTop)) {
								inTagToProcess = true;
								inTagToSkip = false;
								if (tagSet.isIdTag(stackTop))
									inDocnoTag = true;
								else
									inDocnoTag = false;
							} else {
								inTagToProcess = false;
								inTagToSkip = true;
								inDocnoTag = false;
							}
						} else {
							inTagToProcess = false;
							inTagToSkip = false;
							inDocnoTag = false;
						}
					}
					return null;
				}
			} catch (IOException ioe) {
				logger.fatal("Input/Output exception during reading tokens.", ioe);
			}
		}
		if (textCounter == text.length()) {
			EOF = true;
			EOD = true;
		}
		//if the token is not a tag or a document number, then check whether it
		// should be removed or not.
		if (!btag & !docnumber)
			return check(s);
		else
			return s;
	}
	
	/**
	 * The encounterd tag, which must be a final tag is matched with the tag on
	 * the stack. If they are not the same, then the consistency is restored by
	 * popping the tags in the stack, the observed tag included. The EOD flag can
	 * only be signaled by the textCounter.
	 * 
	 * @param tag The closing tag to be tested against the content of the stack.
	 */
	protected void processEndOfTag(String tag) {
		//if there are no tags in the stack, return
		if (stk.empty())
			return;
		//if the given tag is on the top of the stack then pop it
		if (tag.equals((String) stk.peek()))
			stk.pop();
		else { //else report an error, and find the tag.
			if (!ignoreMissingClosingTags) {
				logger.warn("<" + (String) stk.peek()
						+ "> has no closing tag");
				logger.warn("<" + tag + "> not expected");
			}
			int counter = 0;
			int x = stk.search(tag);
			while (!stk.empty() & counter < x) {
				counter++;
				stk.pop();
			}
		}
		//if the stack is empty, this signifies the end of a document.
		if (stk.empty() && textCounter == text.length())
			EOD = true;
	}
	
	/**
	 * Sets the input of the tokenizer.
	 */
	public void setInput(String text) {
		this.text = text;
		//System.out.println("text.length(): "+text.length());
		this.textCounter = 0;
	}
}
