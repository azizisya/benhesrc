package uk.ac.gla.terrier.applications;
public interface EventRegister
{
	/** Register a handler for the given eventName */
	public void registerEvent(String eventName, EventReceiver receiver);
}
