package sk.upjs.drivingSchool.controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import jfxtras.icalendarfx.VCalendar;
import jfxtras.icalendarfx.components.VEvent;
import jfxtras.icalendarfx.properties.component.descriptive.Categories;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.icalendar.ICalendarAgenda;
import sk.upjs.drivingSchool.App;
import sk.upjs.drivingSchool.UserFxModel;
import sk.upjs.drivingSchool.entity.AvailableTime;
import sk.upjs.drivingSchool.entity.Reservation;
import sk.upjs.drivingSchool.entity.Role;
import sk.upjs.drivingSchool.entity.User;
import sk.upjs.drivingSchool.login.UserSessionManager;
import sk.upjs.drivingSchool.persistent.AvailableTimesDao;
import sk.upjs.drivingSchool.persistent.DaoFactory;
import sk.upjs.drivingSchool.persistent.UserDao;

@SuppressWarnings("restriction")
public class AvailableTimesController {

	@FXML
	private ImageView userImageView;

	@FXML
	private Label currentUserName;

	@FXML
	private Button editMyProfileButton;

	@FXML
	private Button changePasswordButton;

	@FXML
	private Button avaibleTimesButton;

	@FXML
	private Button homeButton;

	@FXML
	private Button showUsersButton;

	@FXML
	private Button signOutButton;

	@FXML
	private Button weekLeftButton;

	@FXML
	private Button weekRightButton;

	@FXML
	private Agenda calendarOriginal;
	// musi byt v strede BorderPane,
	// kalendar sa da nastavit iba cez konstruktor
	// preto sa zapamata BorderPane
	// a vzdy sa v nom vytvori novy calendar
	// do ktoreho sa nahadzu hodnoty
	private BorderPane borderPane = null;
	private ICalendarAgenda calendarAgenda;
	private VCalendar myCalendar;

	@FXML
	private Button saveButton;

	@FXML
	private CheckBox activeCheckBox;

	@FXML
	private ComboBox<String> roleComboBox;

	@FXML
	private ComboBox<User> nameComboBox;

	@FXML
	private CheckBox showAllStudentsAvailableTime;
	@FXML
	private CheckBox showAllTeachersAvailableTime;


	private boolean showAllStudentsAvailableTimeChecked = false;
	private boolean showAllTeachersAvailableTimeChecked = false;
	
	

	private AvailableTimesDao availableTimesDao = DaoFactory.INSTANCE.getAvailableTimesDao();
	private UserDao userDao = DaoFactory.INSTANCE.getUserDao();
	private UserFxModel userModel;
	private String selectedRole;
	private boolean selectedActive;
	private User loggedInUser;

	private void initializeUser() {
		long userId = UserSessionManager.INSTANCE.getCurrentUserSession().getUserId();
		loggedInUser = userDao.get(userId);
		if (loggedInUser.getRole().equals(Role.STUDENT.getName())) {
			userImageView
					.setImage(returnImage("src\\main\\resources\\sk\\upjs\\drivingSchool\\pics\\Student-3-icon.png"));
		}
		if (loggedInUser.getRole().equals(Role.TEACHER.getName())) {
			userImageView.setImage(returnImage("src\\main\\resources\\sk\\upjs\\drivingSchool\\pics\\Boss-3-icon.png"));
		}
		if (loggedInUser.getRole().equals(Role.ADMIN.getName())) {
			userImageView.setImage(
					returnImage("src\\main\\resources\\sk\\upjs\\drivingSchool\\pics\\avatar-default-icon.png"));
		}

		currentUserName.setText(loggedInUser.getUsername() + " Role: " + loggedInUser.getRole());
	}

	@FXML
	void initialize() {

		initializeUser();
		this.userModel = new UserFxModel(loggedInUser);
		selectedRole = userModel.getRole();
		selectedActive = userModel.getActive();
		initializeComponentsWithCurrectUserModel();
		initializeCalendar();

		if (userModel.getUser().getRole().equals(Role.STUDENT.getName())) {
			showUsersButton.setDisable(true);
			// showUsersButton.setVisible(false);
		}

		leftMenuInitialize();
		saveButtonAction();
		weekLeftButtonAction();
		weekRightButtonAction();

		showAllTeachersAvailableTime.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				showAllTeachersAvailableTimeChecked = newValue;

				if (showAllTeachersAvailableTimeChecked  ) {
					// saveButton.setDisable(true);
					
					if (borderPane == null)
						borderPane = (BorderPane) calendarOriginal.getParent();
					if (borderPane.getChildren() == null || borderPane.getChildren().size() == 0) {

					} else if (borderPane.getChildren().size() == 1) {
						borderPane.getChildren().remove(0);
					} else {
						System.out.println("Viac kalendarov naraz exception!");
					}

					StringBuilder sb = new StringBuilder();
					DateTimeFormatter formatter;
					sb.append("BEGIN:VCALENDAR\r\n");
					HashSet<AvailableTime> reservationSet = new HashSet<AvailableTime>();

					reservationSet = availableTimesDao.getAllTeachersCalendarEvents();
					if (showAllStudentsAvailableTimeChecked) {
						reservationSet.addAll(availableTimesDao.getAllStudentsCalendarEvents());
					}
					
					for (AvailableTime reservation : reservationSet) {
						sb.append(reservation.getEventString());
						sb.append("\r\n");
					}

					sb.append("END:VCALENDAR");
					String s = sb.toString();
					s.replaceAll("//", "/");

					myCalendar = VCalendar.parse(s);

					calendarAgenda = new ICalendarAgenda(myCalendar);
					borderPane.setCenter(calendarAgenda);

				} else {
					if (showAllStudentsAvailableTimeChecked) {
						if (borderPane == null)
							borderPane = (BorderPane) calendarOriginal.getParent();
						if (borderPane.getChildren() == null || borderPane.getChildren().size() == 0) {

						} else if (borderPane.getChildren().size() == 1) {
							borderPane.getChildren().remove(0);
						} else {
							System.out.println("Viac kalendarov naraz exception!");
						}

						StringBuilder sb = new StringBuilder();
						DateTimeFormatter formatter;
						sb.append("BEGIN:VCALENDAR\r\n");
						HashSet<AvailableTime> reservationSet = new HashSet<AvailableTime>();

						reservationSet = availableTimesDao.getAllStudentsCalendarEvents();
						if (showAllTeachersAvailableTimeChecked) {
							reservationSet.addAll(availableTimesDao.getAllTeachersCalendarEvents());
						}
						
						for (AvailableTime reservation : reservationSet) {
							sb.append(reservation.getEventString());
							sb.append("\r\n");
						}

						sb.append("END:VCALENDAR");
						String s = sb.toString();
						s.replaceAll("//", "/");

						myCalendar = VCalendar.parse(s);

						calendarAgenda = new ICalendarAgenda(myCalendar);
						borderPane.setCenter(calendarAgenda);
					} else {
						refreshNameComboBox();
					}
					// saveButton.setDisable(false);
				//	refreshNameComboBox();
				}
			}
		});

		
		showAllStudentsAvailableTime.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				showAllStudentsAvailableTimeChecked = newValue;

				if (showAllStudentsAvailableTimeChecked) {
					// saveButton.setDisable(true);

					if (borderPane == null)
						borderPane = (BorderPane) calendarOriginal.getParent();
					if (borderPane.getChildren() == null || borderPane.getChildren().size() == 0) {

					} else if (borderPane.getChildren().size() == 1) {
						borderPane.getChildren().remove(0);
					} else {
						System.out.println("Viac kalendarov naraz exception!");
					}

					StringBuilder sb = new StringBuilder();
					DateTimeFormatter formatter;
					sb.append("BEGIN:VCALENDAR\r\n");
					HashSet<AvailableTime> reservationSet = new HashSet<AvailableTime>();

					reservationSet = availableTimesDao.getAllStudentsCalendarEvents();
					if (showAllTeachersAvailableTimeChecked) {
						reservationSet.addAll(availableTimesDao.getAllTeachersCalendarEvents());
					}
					
					for (AvailableTime reservation : reservationSet) {
						sb.append(reservation.getEventString());
						sb.append("\r\n");
					}

					sb.append("END:VCALENDAR");
					String s = sb.toString();
					s.replaceAll("//", "/");

					myCalendar = VCalendar.parse(s);

					calendarAgenda = new ICalendarAgenda(myCalendar);
					borderPane.setCenter(calendarAgenda);

				} else {
					if (showAllTeachersAvailableTimeChecked) {
						if (borderPane == null)
							borderPane = (BorderPane) calendarOriginal.getParent();
						if (borderPane.getChildren() == null || borderPane.getChildren().size() == 0) {

						} else if (borderPane.getChildren().size() == 1) {
							borderPane.getChildren().remove(0);
						} else {
							System.out.println("Viac kalendarov naraz exception!");
						}

						StringBuilder sb = new StringBuilder();
						DateTimeFormatter formatter;
						sb.append("BEGIN:VCALENDAR\r\n");
						HashSet<AvailableTime> reservationSet = new HashSet<AvailableTime>();

						reservationSet = availableTimesDao.getAllTeachersCalendarEvents();
						if (showAllStudentsAvailableTimeChecked) {
							reservationSet.addAll(availableTimesDao.getAllStudentsCalendarEvents());
						}
						
						for (AvailableTime reservation : reservationSet) {
							sb.append(reservation.getEventString());
							sb.append("\r\n");
						}

						sb.append("END:VCALENDAR");
						String s = sb.toString();
						s.replaceAll("//", "/");

						myCalendar = VCalendar.parse(s);

						calendarAgenda = new ICalendarAgenda(myCalendar);
						borderPane.setCenter(calendarAgenda);
					} else {
						refreshNameComboBox();
					}
					// saveButton.setDisable(false);
					//refreshNameComboBox();
				}
			}
		});
	}

	private void weekRightButtonAction() {
		weekRightButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				LocalDateTime l = calendarAgenda.getDisplayedLocalDateTime().plus(Period.ofWeeks(1));
				calendarAgenda.setDisplayedLocalDateTime(l);
			}
		});
	}

	private void weekLeftButtonAction() {
		weekLeftButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				LocalDateTime l = calendarAgenda.getDisplayedLocalDateTime().minus(Period.ofWeeks(1));
				calendarAgenda.setDisplayedLocalDateTime(l);
			}
		});
	}

	private void saveButtonAction() {
		saveButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				saveAvailableTimes();
				userDao.get(loggedInUser.getId()).setLastModified(LocalDateTime.now());
				initializeCalendar();

			}
		});
	}

	private void leftMenuInitialize() {
		homeButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				App.switchScene(new ReservationController(), "ReservationScreen.fxml");
			}
		});
		editMyProfileButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				UserEditController editController = new UserEditController(loggedInUser);
				App.showModalWindow(editController, "UserEdit.fxml");
				// tento kod sa spusti az po zatvoreni okna
				refreshNameComboBox();
			}
		});

		avaibleTimesButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				App.switchScene(new AvailableTimesController(), "AvailableTimes.fxml");
			}
		});
		showUsersButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				App.switchScene(new UserController(), "Listview.fxml");
			}
		});
		signOutButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				App.switchScene(new RegisterSceneController(), "RegisterScreen.fxml");
			}
		});
	}

	private void initializeComponentsWithCurrectUserModel() {

		List<String> roles = Role.STUDENT.getAllNames();
		ObservableList<String> f = FXCollections.observableList(roles);
		roleComboBox.setItems(f);
		roleComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				selectedRole = newValue;
				refreshNameComboBox();
			}
		});
		roleComboBox.getSelectionModel().select(userModel.getRole());

		activeCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				selectedActive = newValue;
				refreshNameComboBox();
			}
		});
		activeCheckBox.selectedProperty().setValue(userModel.getActive());

		List<User> users = userDao.getAll(userModel.getRole(), userModel.getActive());
		nameComboBox.setItems(FXCollections.observableList(users));
		nameComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<User>() {

			@Override
			public void changed(ObservableValue<? extends User> observable, User oldValue, User newValue) {
				if (newValue == null) {
					userModel = null;
				} else {
					userModel = new UserFxModel(newValue);
				}
				initializeCalendar();
				checkSaveButtonVisibility();
			}
		});
		nameComboBox.getSelectionModel().select(userModel.getUser());
	}

	private void refreshNameComboBox() {

		List<User> users = userDao.getAll(selectedRole, selectedActive);
		nameComboBox.setItems(FXCollections.observableList(users));
		if (!users.isEmpty()) {
			nameComboBox.getSelectionModel().select(users.get(0));
		}
		initializeCalendar();
		checkSaveButtonVisibility();
	}

	private void checkSaveButtonVisibility() {

		if (userModel != null && loggedInUser.getId() == userModel.getUser().getId()) {
			// saveButton.setDisable(false);
			saveButton.setVisible(true);
		} else {
			// saveButton.setDisable(true);
			saveButton.setVisible(false);
		}
	}

	private void initializeCalendar() {

		if (borderPane == null)
			borderPane = (BorderPane) calendarOriginal.getParent();
		if (borderPane.getChildren() == null || borderPane.getChildren().size() == 0) {

		} else if (borderPane.getChildren().size() == 1) {
			borderPane.getChildren().remove(0);
		} else {
			System.out.println("Viac kalendarov naraz exception!");
		}
		if (userModel != null) {
			myCalendar = VCalendar.parse(availableTimesToString());
			calendarAgenda = new ICalendarAgenda(myCalendar);
			borderPane.setCenter(calendarAgenda);
		}
	}

	private String availableTimesToString() {
		StringBuilder sb = new StringBuilder();
		DateTimeFormatter formatter;
		sb.append("BEGIN:VCALENDAR\r\n");
		for (AvailableTime availableTime : availableTimesDao.getAvailableTimesByUserId(userModel.getUser().getId())) {// userModel.getAvailableTimes()

			sb.append(availableTime.getEventString());
			sb.append("\r\n");

		}
		sb.append("END:VCALENDAR");
		return sb.toString();
	}

	private void saveAvailableTimes() {

		UserFxModel helperModel = userModel;

		HashSet<AvailableTime> hashSetOfAvailableTimes = new HashSet<>();

		HashSet<String> hashSetOfAvailableTimes2 = availableTimesDao.getAllCalendarEventsUID();

		// System.out.println(hashSetOfAvailableTimes2);
		for (VEvent event : myCalendar.getVEvents()) {
			if (hashSetOfAvailableTimes2.contains(event.getUniqueIdentifier().getValue())) {
				continue;
			}

			event.setSummary("Free Time \r\n" + helperModel.getFname() + helperModel.getLname());
			AvailableTime newAvailableTime = new AvailableTime();
			newAvailableTime.setUserId(loggedInUser.getId());

			String str = event.getDateTimeStart().getValue().toString();
			LocalDateTime l = LocalDateTime.parse(str.substring(0, str.indexOf('+')));
			newAvailableTime.setStartTime(l);

			str = event.getDateTimeEnd().getValue().toString();
			l = LocalDateTime.parse(str.subSequence(0, str.indexOf('+')));
			newAvailableTime.setEndTime(l);

			newAvailableTime.setEventString(event.toString());
			newAvailableTime.setEventStringUID(event.getUniqueIdentifier().getValue());

			hashSetOfAvailableTimes.add(newAvailableTime);

		}

		HashSet<AvailableTime> newAvailableTimeSet = new HashSet<>();
		for (VEvent calendarEvent : myCalendar.getVEvents()) {
			for (AvailableTime modelavailableTime : helperModel.getAvailableTimes()) {
				VEvent modelEvent = VEvent.parse(modelavailableTime.getEventString());
				if (calendarEvent.getUniqueIdentifier().getValue()
						.equals(modelEvent.getUniqueIdentifier().getValue())) {
					modelavailableTime.setEventString(calendarEvent.toString());
					newAvailableTimeSet.add(modelavailableTime);
					// break;
				}
			}
		}

		hashSetOfAvailableTimes.addAll(newAvailableTimeSet);

		availableTimesDao.saveAvailableTimesWithUserId(hashSetOfAvailableTimes, loggedInUser.getId());
		helperModel.setAvailableTimes(hashSetOfAvailableTimes);

		// aby po save zobrazilo uz iba mna
		if (showAllStudentsAvailableTime.isSelected()) {
			showAllStudentsAvailableTime.setSelected(false);
		}

	}

	// skopirovana z
	// https://blog.idrsolutions.com/2012/11/convert-bufferedimage-to-javafx-image/
	private WritableImage returnImage(String path) {
		BufferedImage bf = null;
		try {
			bf = ImageIO.read(new File(path));
		} catch (IOException e) {
			System.out.println("sda");
		}
		WritableImage wr = null;
		if (bf != null) {
			wr = new WritableImage(bf.getWidth(), bf.getHeight());
			PixelWriter pw = wr.getPixelWriter();
			for (int x = 0; x < bf.getWidth(); x++) {
				for (int y = 0; y < bf.getHeight(); y++) {
					pw.setArgb(x, y, bf.getRGB(x, y));
				}
			}
		}
		return wr;
	}

}
