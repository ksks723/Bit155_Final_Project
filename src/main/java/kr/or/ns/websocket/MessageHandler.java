package kr.or.ns.websocket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import kr.or.ns.service.ManagerService;
import kr.or.ns.service.ManagerServiceImpl;
import kr.or.ns.service.MessageService;
import kr.or.ns.vo.Message;
import kr.or.ns.vo.Users;

@Configuration
public class MessageHandler extends TextWebSocketHandler {

	@Autowired
	private MessageService service;

	@Autowired
	private ManagerService mservice;

	private static Map<String, WebSocketSession> users = new HashMap<String, WebSocketSession>();

	private void log(String msg) {
		SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = simple.format(new Date());
	}

	// 연결
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		users.put(session.getPrincipal().getName(), session); // userid 와 session 저장
	}

	// 연결해제
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		if (session.getPrincipal() != null) {
			if (users.containsKey(session.getPrincipal().getName())) {
				users.remove(session.getPrincipal().getName()); // 연결해제된 id 삭제
			}
		}
	}

	// 웹소켓 클라이언트가 텍스트 메시지를 전송할 때 호출
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String userid = session.getPrincipal().getName();
		// 사용자가 보낸 텍스트 데이터 추출 후 분기 처리
		if (message.getPayload().equals("login") || message.getPayload().equals("view")) {
			int result = service.getmsgcount(userid);

			// 내가 받은 메시지 개수 추출

			// 사용자 아이디값이 소켓 접속시 users에 넣은 아이디값이 존재하는지 비교
			if (users.containsKey(userid)) {
				// TextMessage객체를 생성해서 클라이언트에 전송할 텍스트 생성
				TextMessage msg = new TextMessage("" + result + "");
				users.get(userid).sendMessage(msg);
				// ★★★ 키값을 통해 사용자한테 부여한 세션값 추출 후 메시지 전송
			}
		} else if (message.getPayload().split(",")[0].equals("all")) { // 전체쪽지
			String content = message.getPayload().split(",")[1];
			List<Users> list = mservice.getMemberList();
			for (int i = 0; i < list.size(); i++) {
				if (!list.get(i).getUser_id().equals("admin")) {

					String receptionid = list.get(i).getUser_id();
					System.out.println(list.size());
					Message savemsg = new Message();
					savemsg.setSenderid("admin"); // 발신인
					savemsg.setContent(content);
					savemsg.setReceptionid(receptionid);// 수신인
					int result2 = service.insertMessage(savemsg);

					int result = service.getmsgcount(receptionid);
					// 수신인이 받은 문자 개수 추출

					if (users.containsKey(receptionid)) {
						TextMessage msg = new TextMessage("" + result + "");
						users.get(receptionid).sendMessage(msg);
						// ★★★ 키값을 통해 사용자한테 부여한 세션값 추출 후 메시지 전송
					}

				}
			}

		} else { // 쪽지 보냈을때 탐(message.jsp에서 데이터 전송받음) / 1:1쪽지

			String receptionid = message.getPayload().split(",")[0];
			String content = message.getPayload().split(",")[1];

			Message savemsg = new Message();
			savemsg.setReceptionid(receptionid);// 수신인
			savemsg.setSenderid(userid); // 발신인
			savemsg.setContent(content);

			int result2 = service.insertMessage(savemsg);

			// ★★★쪽지 테이블에 보낸 쪽지 데이터 삽입
			int result = service.getmsgcount(receptionid);
			// 수신인이 받은 문자 개수 추출
			if (users.containsKey(receptionid)) {
				TextMessage msg = new TextMessage("" + result + "");
				users.get(receptionid).sendMessage(msg);

				// ★★★ 키값을 통해 사용자한테 부여한 세션값 추출 후 메시지 전송
			}

		}

	}

	// 연결에 문제 발생시 실행되는 함수
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		log(session.getPrincipal().getName() + " / " + exception.getMessage());
	}

}
