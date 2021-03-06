package kr.or.ns.service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import kr.or.ns.dao.AjaxRestDao;
import kr.or.ns.util.Mail;
import kr.or.ns.util.Mailer;
import kr.or.ns.util.Tempkey;
import kr.or.ns.vo.Criteria;
import kr.or.ns.vo.Criteria_Board;
import kr.or.ns.vo.Study;
import kr.or.ns.vo.Users;

@Service
public class AjaxServiceImpl implements AjaxService {
	@Autowired
	private SqlSession sqlsession;

	@Inject
	private JavaMailSender mailSender;

	@Autowired
	private Mailer mailer;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

// 이름과 이메일 받아서
// 존재하는 회원인지 확인
	@Override
	public int emailCheck(String user_name, String user_email) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int result = 0;
		HashMap<String, String> map = new HashMap<String, String>();

		map.put("user_name", user_name);
		map.put("user_email", user_email);
		result = dao.emailCheck(map);
		return result;
	}

// (암호키)이메일
// 전송하는 서비스
	@Override
	public String emailSend(String user_email) {
		String key = new Tempkey().getKey(6, false);

		try {
			Mail mail = new Mail();
			mail.setMailFrom("nosangcoding@gmail.com");
			mail.setMailTo(user_email);
			mail.setMailSubject("[이메일 인증번호 --노상코딩단]");
			mail.setTemplateName("forID.vm");
			mailer.sendMail(mail, key);

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return key;
	}

//id 찾아주는 함수
	@Override
	public String findId(String user_name, String user_email) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);

		HashMap<String, String> map = new HashMap();

		map.put("user_name", user_name);
		map.put("user_email", user_email);

		Users vo = dao.searchId(map);
		String id = vo.getUser_id();
		return id;
	}

//임시비밀번호 발급해주는 로직 
	@Override
	public void makeNewPw(String userid, String useremail) {
		// 입력받은 id, email 있는지 확인
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		Users vo = new Users();
		vo.setUser_id(userid);
		vo.setUser_email(useremail);
		HashMap<String, String> map = new HashMap();
		map.put("user_id", userid);
		map.put("user_email", useremail);

		String key = new Tempkey().getKey(10, false);
		String temp_pw = key;
		vo.setUser_pwd(this.bCryptPasswordEncoder.encode(temp_pw));
		dao.updatePw(vo);

		try {
			Mail mail = new Mail();
			mail.setMailFrom("nosangcoding@gmail.com");
			mail.setMailTo(vo.getUser_email());
			mail.setMailSubject("[임시비밀번호 발급 --노상코딩단]");
			mail.setTemplateName("forPWD.vm");

			mailer.sendMail(mail, key);

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

//ID 중복체크 해주는 로직
	public int idcheck(String user_id) throws ClassNotFoundException {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int result = dao.idcheck(user_id);
		return result;
	}

//받아온 아이디, 이메일로 id 확인	
	@Override
	public int searchId(String user_id, String user_email) {

		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);

		HashMap<String, String> map = new HashMap();

		map.put("user_id", user_id);
		map.put("user_email", user_email);
		int result = 0;
		result = dao.checkEmail(map);

		return result;
	}

// 스터디 지원하기 인서트
	@Override
	public int applyNomalStudy(String s_seq, String user_id) {
		
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		//insert 정보넘길 맵생성
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("s_seq", s_seq);
				map.put("user_id", user_id);
		int a_staCount = dao.checkA_staCount(s_seq);
		int people = dao.checkPeople(s_seq);
			//insert 하러 간다 
			int insertResult = dao.insertStudyGroup(map);
			return insertResult;
	}

	// 신고하기
	@Override
	public int blameInsert(HashMap<String, Object> params, String current_userid) {
		String s_seq = (String) params.get("s_seq"); // 글번호(게시판)
		String m_seq = (String) params.get("m_seq"); // 글번호(쪽지)
		String btc_seq = (String) params.get("type"); // 신고유형
		String bpc_seq = (String) params.get("place"); // 신고장소(게시판이면 1, 쪽지면 2 디폴트)
		String writer = (String) params.get("target"); // 신고당하는 사람(글작성자,해당 게시글)
		String title = (String) params.get("bl_title"); // 신고제목
		String comment = (String) params.get("comment"); // 신고내용
		String blamed_title = (String) params.get("blamed_title");
		String blamed_content = (String) params.get("blamed_content");
		
		HashMap map_board = new HashMap(); // 게시판용 map
		map_board.put("board_seq", s_seq);// 글번호(글번호)
		map_board.put("current_userid", current_userid); // 신고자
		map_board.put("btc_seq", btc_seq); // 신고유형
		map_board.put("bpc_seq", bpc_seq); // 신고장소
		map_board.put("bl_target_id", writer); // 신고당하는사람
		map_board.put("bl_title", title); // 신고제목
		map_board.put("bl_content", comment); // 신고내용
		map_board.put("blamed_title", blamed_title); //신고당한 글 제목
		map_board.put("blamed_content", blamed_content); //신고당한 글 내용

		HashMap map_message = new HashMap(); // 쪽지용 map
		map_message.put("board_seq", m_seq); // 글번호(게시판)
		map_message.put("current_userid", current_userid); // 신고자
		map_message.put("btc_seq", btc_seq); // 신고유형
		map_message.put("bpc_seq", bpc_seq); // 신고장소
		map_message.put("bl_target_id", writer); // 신고당하는사람
		map_message.put("bl_title", title); // 신고제목
		map_message.put("bl_content", comment); // 신고내용
		map_message.put("blamed_content", blamed_content); //신고당한 쪽지 내용

		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);

		int result = 0;

		if (bpc_seq.equals("1")) {
			// 게시판
			result = dao.insertBlame(map_board);
		} else {
			// 쪽지
			result = dao.insertBlame_Message(map_message);
		}

		return result;
	}

	// 쪽지삭제
	@Override
	public int deleteMessage(HashMap<String, Object> params) {
		String m_seq = (String) params.get("m_seq"); // 글번호

		HashMap map = new HashMap();
		map.put("m_seq", m_seq);

		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int result = 0;
		result = dao.delete_Message(map);
		return result;
	}

	// 유저정보 모달창에 뿌리기
	@Override
	public List<HashMap<String, Object>> userInfoModal(HashMap<String, Object> params) {
		String user_id = (String) params.get("user_id");

		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		List<HashMap<String, Object>> userInfo = dao.getUserInfo(user_id);
		return userInfo;
	}
	
	//유저정보 모달에 게시판뿌리기
	@Override
	public List<HashMap<String, Object>> userBoardModal(HashMap<String, Object> params) {
		String user_id = (String) params.get("user_id");

		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		List<HashMap<String, Object>> userBoard = dao.getUserBoardInfo(user_id);
		return userBoard;
	}

	// 이메일 중복체크
	@Override
	public int onlyEmailCheck(String user_email) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int result = dao.onlyEmailCheck(user_email);
		return result;
	}

	// 마이페이지 모집중 스터디 비동기
	@Override
	public List<HashMap<String, Object>> recrutingStudy(HashMap<String, Object> params) {
		String user_id = (String) params.get("user_id");

		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		List<HashMap<String, Object>> list = dao.recrutingStudy(user_id);
		return list;
	}

	// 마이페이지 참여중 스터디 비동기
	@Override
	public List<HashMap<String, Object>> inStudy(HashMap<String, Object> params) {

		String user_id = (String) params.get("user_id");
	
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		List<HashMap<String, Object>> list = dao.inStudy(user_id);
		return list;

	}

	@Override
	public List<HashMap<String, Object>> mainChart() {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		List<HashMap<String, Object>> list = dao.mainChart();
		return list;
	}

	// 지원현황 승인 후 승인완료 데이터 반환
	public List<HashMap<String, Object>> accept(HashMap<String, Object> params) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int a = dao.accept(params);
		List<HashMap<String, Object>> list = null;
		if (a == 1) {
			list = dao.acceptList(params);
		}
		return list;
	}
	
	//승인 거절(Delete 치기)
	public int reject(HashMap<String, Object> params) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int a = dao.reject(params);
		return a;
	}
	
	//참가중인 스터디원 취소
	public List<HashMap<String, Object>> cancel(HashMap<String, Object> params){
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int a = dao.cancel(params);
		List<HashMap<String, Object>> list = null;
		if (a == 1) {
			list = dao.cancelList(params);
		}
		return list;
	}
	
	public int deleteBookMark(HashMap<String, Object> params) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int result = dao.deleteBookMark(params);
	
		return result;
	}
	
	//스터디 게시판 필터
	public List<HashMap<String, Object>> studyBoardFilter(HashMap<String, Object> params, Criteria_Board cri_b){
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
	
		params.put("pageStart", cri_b.getPageStart());
		params.put("perPageNum", cri_b.getPerPageNum());
		List<HashMap<String, Object>> result = dao.studyBoardFilter(params);
		return result;
	}
	
	//스터디 게시판 필터 사이즈 체크용
	public List<HashMap<String, Object>> studyBoardFilterSize(HashMap<String, Object> params){
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
	
		List<HashMap<String, Object>> result = dao.studyBoardFilterSize(params);
		return result;
	}
	
	//강의 게시판 필터
	public List<HashMap<String, Object>> courseBoardFilter(HashMap<String, Object> params, Criteria cri_b){
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		
		params.put("pageStart", cri_b.getPageStart());
		params.put("perPageNum", cri_b.getPerPageNum());
		List<HashMap<String, Object>> result = dao.courseBoardFilter(params);
		return result;
	}
	
	//스터디 게시판 필터 사이즈 체크용
		public List<HashMap<String, Object>> courseBoardFilterSize(HashMap<String, Object> params){
			AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
			List<HashMap<String, Object>> result = dao.courseBoardFilterSize(params);
		
			return result;
		}

	
	//내가 쓴 댓글 리스트
	@Override
	public List<HashMap<String, Object>> commentList(HashMap<String, Object> params) {
		String user_id = (String) params.get("user_id");
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		List<HashMap<String, Object>> list = dao.commentList(user_id);
		return list;
	}
	
	//모집마감
	@Override
	public void finishRecruit(String s_seq) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		dao.finishRecruit(s_seq);
	}

	//모집마감으로 변경시 승인대기중 회원목록 삭제
	@Override
	public void deleteWaitingUsers(String s_seq) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		dao.deleteWaitingUsers(s_seq);
	}

	//스터디 지원한거 취소하기 
	@Override
	public void applycancelNomalStudy(String s_seq, String user_id) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		dao.applycancelNomalStudy(s_seq,user_id);
	}
	
	//워드클라우드 차트
	@Override
	public List<HashMap<String, Object>> wordCloud() {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		List<HashMap<String, Object>> list = dao.wordCloud();
		return list;
	}
	
	//소셜계정용 권한체크
	@Override
	public int enabledcheck(String user_id) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		int enabled = dao.enabledcheck(user_id);
		return enabled;
	}
	
	public List<HashMap<String, Object>> userInfoChat(@RequestBody HashMap<String, Object> params) {
		String user_id = (String) params.get("user_id");
	
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		List<HashMap<String, Object>> userChatInfo = dao.getUserInfo(user_id);
		return userChatInfo;
	}

	@Override
	public List<HashMap<String, Object>> getAutoKeyword(String keyword) {

		List<HashMap<String, Object>> list = null;
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		list = dao.getAutoKeyword(keyword);	
		return list;
	}

	//승인 완료된 인원수 체크 
	@Override
	public int checkA_staCount(String s_seq) {
		AjaxRestDao dao = sqlsession.getMapper(AjaxRestDao.class);
		
		//승인 완료된 인원수 체크 한다 
		int result = dao.checkA_staCount(s_seq);
		int recruitingpeople = dao.checkPeople(s_seq);
		if(result > recruitingpeople) {
			//모집정원보다 승인완료 인원이 크다면 return 0;
			return 0;
		}else {
			return 1;
		}
	}
}
