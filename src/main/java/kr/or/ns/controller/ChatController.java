package kr.or.ns.controller;

import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.or.ns.service.ChatService;
import kr.or.ns.service.MyPageService;
import kr.or.ns.vo.ChatRoom;
import kr.or.ns.vo.ChatRoomMember;
import kr.or.ns.vo.Users;

@Controller
@RequestMapping("/chat/")
public class ChatController {

	@Autowired
	private ChatService service;

	@Autowired
	private MyPageService mservice;

	// 채팅리스트방 입장
	@RequestMapping("roomlist.do")
	public String roomListPage(Model model) throws ClassNotFoundException, SQLException {
		List<HashMap<String, Object>> roomList = service.getListChatRoom();
		model.addAttribute("roomList", roomList);
		return "chat/roomlist";
	}

	@RequestMapping("chatroominsert.do")
	@ResponseBody
	public List<HashMap<String, Object>> chatRoomInsert(@RequestBody Map<String, Object> params, Principal principal)
			throws IOException {

		if (params.get("ch_pw") != null) {
			params.put("ch_pw_check", 1);
		} else {
			params.put("ch_pw_check", 0);
		}
		params.put("user_id", principal.getName());
		service.registerRoom(params);
		List<HashMap<String, Object>> list = service.getListChatRoom();
		return list;
	}

	// 채팅방 내부 입장 , 채팅멤버 테이블에 삽입
	@RequestMapping("entrance.do")
	public String chatRoomEntrance(@RequestParam HashMap<Object, Object> params, Model model, Principal principal)
			throws ClassNotFoundException, SQLException {

		// 현재날짜 포맷팅해서 구하기
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
		String datestr = sdf.format(cal.getTime());

		String ch_seq = (String) params.get("ch_seq");
		String user_id = principal.getName();

		Users user = mservice.getUsers(user_id);
		String nickname = user.getNickname();
		
		ChatRoomMember cm = new ChatRoomMember();
		cm.setCh_seq(Integer.parseInt(ch_seq));
		cm.setUser_id(user_id);
		int result = service.memberInsert(cm);
		ChatRoom chatroom = service.getChatRoom(ch_seq);

		List<HashMap<String, Object>> list = service.chatRoomMemberGet(ch_seq);
		ChatRoom cr = service.getChatRoom(ch_seq);
		String master = cr.getUser_id();

		model.addAttribute("master", master);
		model.addAttribute("chatroom", chatroom);
		model.addAttribute("user_id", user_id);
		model.addAttribute("nickname", nickname);
		model.addAttribute("datestr", datestr);

		return "chat/chatroom";
	}

	@RequestMapping("chatRoomOut.do")
	public String chatRoomOut(String ch_seq, Model model, Principal principal)
			throws ClassNotFoundException, SQLException {

		String user_id = principal.getName();

		ChatRoomMember cm = new ChatRoomMember();
		cm.setCh_seq(Integer.parseInt(ch_seq));
		cm.setUser_id(user_id);
		
		ChatRoom cr = service.getChatRoom(ch_seq);
		String master = cr.getUser_id();
		
		if(!user_id.equals(master)) { 
			int result = service.chatRoomOut(cm);
		}
	
		List<HashMap<String, Object>> roomList = service.getListChatRoom();
		model.addAttribute("roomList", roomList);

		return "chat/roomlist";
	}

	// 비밀방의 방번호를 받아서 패스워드 넘겨줌
	@RequestMapping(value = "roomPw.ajax", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody Map<String, String> roomPw(@RequestParam HashMap<Object, Object> params) {
		String pw = "";
		Map<String, String> map = new HashMap<String, String>();
		String ch_seq = (String) params.get("ch_seq");
		if (ch_seq != null && !ch_seq.trim().equals("")) {
			pw = service.roomPw(ch_seq);
			map.put("pw", pw);
		}
		return map;
	}

	// 채팅방 내부에서 멤버리스트 보는 페이지로 이동
	@RequestMapping("chatmemberlist.do")
	public String chatmemberlist() throws ClassNotFoundException, SQLException {
		return "chat/memberlist";
	}

	// 채팅방 내부에서 멤버리스트 보는 페이지로 이동
	@RequestMapping("chatDelete.do")
	public String chatDelete(String ch_seq) throws ClassNotFoundException, SQLException {
		int result = service.chatDelete(ch_seq);
		return "redirect:/chat/roomlist.do";
	}

	// 채팅방 회원 참여현황 불러오기
	@RequestMapping("chatRoomMemberGet.do")
	@ResponseBody
	public List<HashMap<String, Object>> chatRoomMemberGet(@RequestBody Map<String, Object> params) throws IOException {
		String ch_seq = (String) params.get("ch_seq");
		List<HashMap<String, Object>> list = service.chatRoomMemberGet(ch_seq);
		return list;
	}
	
	// 채팅방 정보 수정
	@RequestMapping("chatUpdate.do")
	@ResponseBody
	public void chatUpdate(@RequestBody Map<String, Object> params) throws IOException {
		int result = service.chatUpdate(params);
		
	}
	


}
