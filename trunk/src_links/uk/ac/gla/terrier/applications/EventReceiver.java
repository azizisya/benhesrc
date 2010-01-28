package uk.ac.gla.terrier.applications;
public interface EventReceiver
{
	public void invokeEvent(String eventName, String[] data);

	public void selfRegisterEvents(EventRegister er);
}
