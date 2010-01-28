package uk.ac.gla.terrier.terms;
import javax.swing.JTextArea;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;

public class SwingDumpTerm extends JFrame implements TermPipeline 
{
	/** Stop putting stuff on the screen */
	protected volatile boolean PausedUpdates = false;
	/** Next item in the term pipeline to pass onto */
	protected TermPipeline next = null;
	/** JTextArea to display the terms on. */
	protected JTextArea textarea = null;
	/** Put the textarea in a scroll pane so we can scroll back to see it */
	protected JScrollPane scrolltextarea = null;
	/** Keep only the last 100 terms */
	protected static int LINE_LIMIT = 100;
	/** Pressing this to stop receiving updated for terms */
	protected JButton btnPause = null;
	/** Overall layout pane - borderlayout is used*/
	JPanel jContentPane = null;

	/** Construct a new SwingDumpTerm object */
	public SwingDumpTerm(TermPipeline next)
	{
		this.next = next;
		
		btnPause = new JButton("Pause");
		btnPause.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (PausedUpdates)
					{
						btnPause.setText("Pause");
						PausedUpdates = false;
					}
					else
					{
						btnPause.setText("Restart");
						PausedUpdates = true;
					}
					
				}
			});
		
		textarea = new JTextArea();
		textarea.setEditable(false);
		textarea.setWrapStyleWord(true);
		
		
		
		scrolltextarea = new JScrollPane();
		scrolltextarea.setViewportView(textarea);
		scrolltextarea.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.gray, 1));
		scrolltextarea.setPreferredSize(new java.awt.Dimension(2, 48));
		scrolltextarea.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		jContentPane = new JPanel();
		jContentPane.setLayout(new java.awt.BorderLayout());
		jContentPane.setPreferredSize(new java.awt.Dimension(0,0));
		jContentPane.add(scrolltextarea, java.awt.BorderLayout.CENTER);
		jContentPane.add(btnPause, java.awt.BorderLayout.SOUTH);
		this.setContentPane(jContentPane);
		
		this.setSize(500, 410);
		this.setTitle("SwingDumpTerm - what terms are you indexing?");
		this.setLocationRelativeTo(null);
		
		this.setVisible(true);
	}
	
	/**
	 * Displays the given term on textarea, then passes onto next pipeline object.
	 * @param t String the term to pass onto next pipeline object
	 */
	public void processTerm(String t)
	{
		if (t == null)
			return;
		int lineCount;
		
		if (! PausedUpdates)
		{
			try {
				if ((lineCount = textarea.getLineCount()) > LINE_LIMIT) {
					textarea.replaceRange("", 0, textarea.getLineStartOffset(lineCount - LINE_LIMIT));
				}
				textarea.append("term: "+t+"\n");
			} catch(BadLocationException ble) {
				System.err.println("Bad location exception:" + ble);
				ble.printStackTrace();
			}
		}
			
		next.processTerm(t);
	}
		
}
