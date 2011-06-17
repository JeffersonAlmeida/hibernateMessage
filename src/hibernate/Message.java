package hibernate;

public class Message {
	
	private Integer id;
	private String text;
	private Message nextMessage;	
	
	public Message() {	}
	
	
	public Message(String text) {
	this.text = text;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Message getNextMessage() {
		return nextMessage;
	}
	public void setNextMessage(Message nextMessage) {
		this.nextMessage = nextMessage;
	}


	@Override
	public String toString() {
		return "Message [id=" + id + ", text=" + text + ", nextMessage="
				+ nextMessage + "]";
	}
	
}
