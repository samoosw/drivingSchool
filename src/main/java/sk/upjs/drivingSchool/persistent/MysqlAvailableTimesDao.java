package sk.upjs.drivingSchool.persistent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import sk.upjs.drivingSchool.entity.AvailableTime;

public class MysqlAvailableTimesDao implements AvailableTimesDao {
	
	private JdbcTemplate jdbcTemplate;

	public MysqlAvailableTimesDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Override
	public HashSet<String> getAllCalendarEventsUID() {
		String sql = "SELECT availabletime.eventStringUID FROM AvailableTime ";
		List<String>data=jdbcTemplate.queryForList(sql,String.class);
		return new HashSet<String>(data);
	}

	@Override
	public HashSet<AvailableTime> getAllCalendarEvents() {
		String sql = "SELECT availabletime.eventString FROM AvailableTime ";
		List<AvailableTime> reservationsList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AvailableTime.class));
		return new HashSet<AvailableTime>(reservationsList);
	}
	
	@Override
	public HashSet<AvailableTime> getAllStudentsCalendarEvents() {
		String sql = "select availabletime.eventString from availabletime join user ON(user.id = availabletime.myUserId)  where user.role = 'student';";
		List<AvailableTime> reservationsList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AvailableTime.class));
		return new HashSet<AvailableTime>(reservationsList);
	}
	
	@Override
	public HashSet<AvailableTime> getAllTeachersCalendarEvents() {
		String sql = "select availabletime.eventString from availabletime join user ON(user.id = availabletime.myUserId)  where user.role = 'teacher';";
		List<AvailableTime> reservationsList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AvailableTime.class));
		return new HashSet<AvailableTime>(reservationsList);
	}
	
	@Override
	public HashSet<AvailableTime> getAvailableTimesByUserId(long userId) {
		String sql = "SELECT time.id, time.myUserId, time.startTime, time.endTime, time.eventString, time.eventStringUID " +
				"FROM availableTime AS time JOIN user ON user.id = time.myUserId " +
				"WHERE user.id = " + userId; 
		List<AvailableTime> availableTimesList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AvailableTime.class));
		return new HashSet<AvailableTime>(availableTimesList);
	}

	@Override
	public void saveAvailableTimesWithUserId(HashSet<AvailableTime> availableTimes, long userId) {
		jdbcTemplate.update("DELETE FROM availableTime WHERE myUserId = ?", userId);	
		for(AvailableTime a : availableTimes) {
			add(a);
		}	
	}
	
	private void add(AvailableTime availableTime) {
		SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
		simpleJdbcInsert.withTableName("availableTime");
		simpleJdbcInsert.usingGeneratedKeyColumns("id");
		simpleJdbcInsert.usingColumns("startTime", "endTime", "myUserId", "eventString", "eventStringUID");
		
		Map<String,Object> hodnoty = new HashMap<>();
		hodnoty.put("startTime",availableTime.getStartTime());
		hodnoty.put("endTime",availableTime.getEndTime());
		hodnoty.put("myUserId",availableTime.getMyUserId());
		hodnoty.put("eventString", availableTime.getEventString());
		hodnoty.put("eventStringUID", availableTime.getEventStringUID());
		
		Long id = simpleJdbcInsert.executeAndReturnKey(hodnoty).longValue();
		availableTime.setId(id);
	}
}
