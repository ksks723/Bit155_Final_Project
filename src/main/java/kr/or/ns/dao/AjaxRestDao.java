package kr.or.ns.dao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import kr.or.ns.vo.Users;

public interface AjaxRestDao {
	//이름과 이메일 받아서 존재하는 회원인지 확인
	public int emailCheck(HashMap<String, String> map) ;
	
	//중복체크
	public int idcheck(String user_id);
	
}
