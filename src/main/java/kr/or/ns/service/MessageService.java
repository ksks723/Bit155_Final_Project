package kr.or.ns.service;

import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ns.vo.Message;

public interface MessageService {
	public int getmsgcount(String userid);
	public int insertMessage(Message message);
	public Message getMessage(String m_seq);
	public List<Message> sendListMessage(String userid);
	public int deleteMessageOne(String m_seq);
	
	
	//목록
//	public List<Message> getListMessage(String userid);
	
	//내 메세지 갯수
	public int getMyMessageCount(String user_id);
	
	//목록+페이징
	public List<HashMap<String, Object>> getMessageList(HashMap<String, Object> map);
	
}
