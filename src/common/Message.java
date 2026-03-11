package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, LOGOUT, BROADCAST, PRIVATE, FILE_REQUEST, FILE_DATA, 
        USER_LIST, ERROR, SUCCESS, CHAT_HISTORY, PING
    }

    private Type type;
    private String sender;
    private String receiver;   // null = broadcast
    private String content;
    private byte[] fileData;
    private String fileName;
    private long fileSize;
    private String timestamp;

    public Message(Type type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // Getters and setters
    public Type getType()             { return type; }
    public String getSender()         { return sender; }
    public String getReceiver()       { return receiver; }
    public String getContent()        { return content; }
    public byte[] getFileData()       { return fileData; }
    public String getFileName()       { return fileName; }
    public long getFileSize()         { return fileSize; }
    public String getTimestamp()      { return timestamp; }

    public void setType(Type type)          { this.type = type; }
    public void setSender(String sender)    { this.sender = sender; }
    public void setReceiver(String receiver){ this.receiver = receiver; }
    public void setContent(String content)  { this.content = content; }
    public void setFileData(byte[] fileData){ this.fileData = fileData; }
    public void setFileName(String fileName){ this.fileName = fileName; }
    public void setFileSize(long fileSize)  { this.fileSize = fileSize; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}
