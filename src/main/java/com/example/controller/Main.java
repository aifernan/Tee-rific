/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.controller;

import com.example.models.BCrypt;
import com.example.models.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

@Controller
@SpringBootApplication
public class Main {

  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }


  @RequestMapping("/")
  String index() {
    return "redirect:/tee-rific/login";
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

//************************************
// TEE-RIFIC CODE STARTS HERE
//************************************

  String[] priorities = {"GOLFER", "OWNER", "ADMIN"};

//**********************
// LOGIN
//**********************

  boolean failedLogin = false;

  @GetMapping(
          path = "/tee-rific/login"
  )
  public String getLoginPage(Map<String, Object> model){

    User user = new User();
    model.put("loginUser", user);

    if (failedLogin == true){
      String error = "Error: Username/Password Doesn't match/exist";
      model.put("failedLogin", error);
      failedLogin = false;
    }
    return "Login&Signup/login";
  }

  @PostMapping(
          path = "/tee-rific/login/check",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String checkLoginInfo(Map<String, Object> model, User user, HttpServletRequest request) throws Exception {

    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (priority varchar(100), username varchar(100), password varchar(100), fname varchar(100), lname varchar(100), email varchar(100), gender varchar(100))");

      // creates and check if admin account created
      int checkIfAdminExists = 0;
      ResultSet checkAdmin = stmt.executeQuery("SELECT * FROM users WHERE username = 'admin'");
      while (checkAdmin.next()){
        checkIfAdminExists++;
      }
      if (checkIfAdminExists == 0){
        String adminPassword = "cmpt276";
        String encryptedAdminPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt());
        String insert = "INSERT INTO users (priority, username, password) VALUES ('ADMIN','admin','"+encryptedAdminPassword+"')";
        stmt.executeUpdate(insert);
      }

      String sql = "SELECT * FROM users WHERE username = '" + user.getUsername() + "'";
      ResultSet rs = stmt.executeQuery(sql);

      int checkIfUserExists = 0;
      String checkPassword = "";
      String priority = "";
      while (rs.next()){
        checkIfUserExists++;
        checkPassword = rs.getString("password");
        priority = rs.getString("priority");

        String encryptedPassword = BCrypt.hashpw(checkPassword, BCrypt.gensalt());

      }
      System.out.println(checkPassword);
      System.out.println(user.getPassword());

      if (checkIfUserExists > 0 && (BCrypt.checkpw(user.getPassword(), checkPassword))){

        request.getSession().setAttribute("username", user.getUsername());
        String userid = (String) request.getSession().getAttribute("username");

        return "redirect:/tee-rific/home/" + userid;
      }
      failedLogin = true;
      return "redirect:/tee-rific/login";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


//**********************
// SIGN-UP
//**********************

  boolean usernameError = false;

  @GetMapping(
          path = "/tee-rific/signup"
  )
  public String getSignupPage(Map<String, Object> model) {
    User user = new User();
    model.put("newUser", user);
    if (usernameError == true){
      String error = "Error: Username already Exists.";
      model.put("usernameError", error);
      usernameError = false;
    }
    return "Login&Signup/signup";
  }


  @PostMapping(
          path = "/tee-rific/signup"
  )
  public String handleBrowserNewUserSubmit(Map<String, Object> model, User user) throws Exception {
    try(Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();

      String encryptedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

      user.setPriority(priorities[0]);      //sets the priority of the user to 'GOLFER'

      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (priority varchar(100), username varchar(100), password varchar(100), fname varchar(100), lname varchar(100), email varchar(100), gender varchar(100))");

      // creates and check if admin account created
      int checkIfAdminExists = 0;
      ResultSet checkAdmin = stmt.executeQuery("SELECT * FROM users WHERE username = 'admin'");
      while (checkAdmin.next()){
        checkIfAdminExists++;
      }
      if (checkIfAdminExists == 0){
        String adminPassword = "cmpt276";
        String encryptedAdminPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt());
        String insert = "INSERT INTO users (priority, username, password) VALUES ('ADMIN','admin','"+encryptedAdminPassword+"')";
        stmt.executeUpdate(insert);
      }

      stmt.executeUpdate("INSERT INTO users (priority, username, password, fname, lname, email, gender) VALUES ('" + user.getPriority() + "','" + user.getUsername() + "','" + encryptedPassword + "','" + user.getFname() + "','" + user.getLname() + "','" + user.getEmail() + "','" + user.getGender() + "')");

      String sql = "SELECT username FROM users WHERE username ='"+user.getUsername()+"'";
      ResultSet rs = stmt.executeQuery(sql);
      int checkCount = 0;
      while (rs.next()){
        checkCount++;
      }

      if (checkCount > 1){
        stmt.executeUpdate("DELETE FROM users WHERE priority='" + user.getPriority() + "' and username='" + user.getUsername() + "' and password='"+ encryptedPassword + "' and fname='"+user.getFname() + "' and lname='"+user.getLname() + "' and email='"+user.getEmail() + "' and gender='"+user.getGender()+"'");
        usernameError = true;
        return "redirect:/tee-rific/signup";
      } else {
        model.put("username", user.getUsername());
        return "LandingPages/success";
      }
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


//**********************
// OWNER SIGN-UP
//**********************

  boolean courseNameError = false;

  @GetMapping(
          path = "/tee-rific/signup/Owner"
  )
  public String getOwnerSignUpPage(Map<String, Object> model){
    CourseOwner owner = new CourseOwner();
    model.put("newOwner", owner);

    if (usernameError == true){
      String error = "Error: Username already Exists.";
      model.put("usernameError", error);
      usernameError = false;
    } else if (courseNameError == true){
      String error = "Error: Course Name already Exists.";
      model.put("courseNameError", error);
      courseNameError = false;
    }
    return "Owner/ownerSignUp";
  }


  @PostMapping(
          path = "/tee-rific/signup/Owner",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String handleBrowserOwnerSubmit(Map<String, Object> model, CourseOwner owner){
    //create a new user, owner, golf course, then add to database

    try(Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();

      String encryptedPassword = BCrypt.hashpw(owner.getPassword(), BCrypt.gensalt());

      //TODO: will need to figure out way to store image into sql later - Mike

      owner.setNumHoles(18);
      String ownerInfo = getSQLNewTableOwner();
      String insertOwners = getSQLInsertOwner(owner, encryptedPassword);

      //add user to database
      stmt.executeUpdate(ownerInfo);
      stmt.executeUpdate(insertOwners);

      //snake case the course name
    /* 'THIS IS BETTER THAN CAMEL CASE' for this situation:
        - cannot have spaces in name or else it breaks the SQL query
        - snake case works best as it makes for easy conversion back to original format
        - camel case is disregarded by the SQL, implying no way of knowing where to split the words to convert back to original format */
      String updatedCourseName = convertToSnakeCase(owner.getCourseName());
      String courseInfo = "CREATE TABLE IF NOT EXISTS " + updatedCourseName + " (holeNumber integer, yardage integer, par integer, handicap integer)";
      stmt.executeUpdate(courseInfo);

      //initializes a table to keep track of the course hole details
      for(int i = 0; i < owner.getNumHoles(); i++){
        String insertHole = "INSERT INTO " + updatedCourseName + "(" + "holeNumber, yardage, par, handicap) VALUES (' " + (i + 1) + "', '0', '0', '0')";
        stmt.executeUpdate(insertHole);
      }

      //add to user table
      String userInfo = getSQLNewTableUsers();

      // creates and check if admin account created
      int checkIfAdminExists = 0;
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (priority varchar(100), username varchar(100), password varchar(100), fname varchar(100), lname varchar(100), email varchar(100), gender varchar(100))");
      ResultSet checkAdmin = stmt.executeQuery("SELECT * FROM users WHERE username = 'admin'");
      while (checkAdmin.next()){
        checkIfAdminExists++;
      }
      if (checkIfAdminExists == 0){
        String adminPassword = "cmpt276";
        String encryptedAdminPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt());
        String insert = "INSERT INTO users (priority, username, password) VALUES ('ADMIN','admin','"+encryptedAdminPassword+"')";
        stmt.executeUpdate(insert);
      }

      //create a user based on owner fields
      User user = new User();
      user.setPriority(priorities[1]);
      user.setUsername(owner.getUsername());
      user.setPassword(owner.getPassword());
      user.setFname(owner.getFname());
      user.setLname(owner.getLname());
      user.setEmail(owner.getEmail());
      user.setGender(owner.getGender());

      String insertUser = getSQLInsertUser(user, encryptedPassword);

      stmt.executeUpdate(userInfo);
      stmt.executeUpdate(insertUser);

      // Initialize rental inventory of golf course - Chino
      ownerCreateInventory(connection);

      // check if username or course name exists for already existing user
      String sql = "SELECT username FROM owners WHERE username ='"+user.getUsername()+"'";
      ResultSet rs = stmt.executeQuery(sql);
      int checkUserCount = 0;
      while (rs.next()){
        checkUserCount++;
      }

      String sqlCH = "SELECT courseName FROM owners WHERE courseName ='"+updatedCourseName+"'";
      ResultSet rsCH = stmt.executeQuery(sqlCH);
      int checkCNCount = 0;
      while (rsCH.next()){
        checkCNCount++;
      }

      if (checkUserCount > 1){
        // delete from user and owner database
        stmt.executeUpdate("DELETE FROM users WHERE priority='" + user.getPriority() + "' and username='" + user.getUsername() + "' and password='"+ encryptedPassword + "' and fname='"+ user.getFname() + "' and lname='"+user.getLname() + "' and email='"+user.getEmail() + "' and gender='"+user.getGender()+"'");
        String deleteOwner = getSQLDeleteOwner(owner, encryptedPassword);
        stmt.executeUpdate(deleteOwner);

        usernameError = true;
        return "redirect:/tee-rific/signup/Owner";
      } else if (checkCNCount > 1){
        stmt.executeUpdate("DELETE FROM users WHERE priority='" + user.getPriority() + "' and username='" + user.getUsername() + "' and password='"+ encryptedPassword + "' and fname='"+ user.getFname() + "' and lname='"+user.getLname() + "' and email='"+user.getEmail() + "' and gender='"+user.getGender()+"'");
        String deleteOwner = getSQLDeleteOwner(owner, encryptedPassword);
        stmt.executeUpdate(deleteOwner);

        courseNameError = true;
        return "redirect:/tee-rific/signup/Owner";
      } else
        model.put("username", user.getUsername());
      return "LandingPages/success";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  String getSQLNewTableOwner() {
    return  "CREATE TABLE IF NOT EXISTS owners (" +
            "courseName varchar(100), address varchar(100), city varchar(100), country varchar(100), website varchar(150), phoneNumber varchar(100), " +
            "courseLogo varchar(150), " +               //TODO: will need to fix this one image storage is figured out - MIKE
            "directionsToCourse varchar(500), description varchar(500), weekdayRates varchar(100), weekendRates varchar(100), numHoles integer, " +
            "userName varchar(100), password varchar(100),firstName varchar(100),lastName varchar(100),email varchar(100),yardage varchar(100),gender varchar(100))";
  }


  private String getSQLInsertOwner(CourseOwner owner, String secretPW){
    return "INSERT INTO owners ( " +
            "courseName, address, city, country, website, phoneNumber, courseLogo, " +
            "directionsToCourse, description, weekdayRates, weekendRates, numHoles, " +
            "userName, password, firstName, lastName, email, yardage, gender) VALUES ('" +
            owner.getCourseName() + "','" + owner.getAddress() + "','" + owner.getCity() + "','" +
            owner.getCountry() + "','" + owner.getWebsite() + "','" + owner.getPhoneNumber() + "','" +
            owner.getCourseLogo() + "','" + owner.getDirectionsToCourse() + "','" + owner.getDescription() + "','" +
            owner.getWeekdayRates() + "','" +  owner.getWeekendRates() + "','" + owner.getNumHoles() + "','" +
            owner.getUsername() + "','" + secretPW + "','" + owner.getFname() + "','" + owner.getLname() + "','" +
            owner.getEmail() + "','" + owner.getYardage() + "', '" + owner.getGender() + "')";
  }


  private String getSQLDeleteOwner(CourseOwner owner, String secretPW){
    return "DELETE FROM owners WHERE courseName='" + owner.getCourseName() + "' and address='" + owner.getAddress() +
            "' and city='" + owner.getCity() +  "' and country='" + owner.getCountry() + "' and website='"  +
            owner.getWebsite() + "' and phoneNumber='" + owner.getPhoneNumber() + "' and courseLogo='" +
            owner.getCourseLogo() + "' and directionsToCourse='" + owner.getDirectionsToCourse() + "' and description='" +
            owner.getDescription() + "' and weekdayRates='" + owner.getWeekdayRates() + "' and weekendRates='" +
            owner.getWeekendRates() + "' and numHoles='" + owner.getNumHoles() + "' and userName='" + owner.getUsername() +
            "' and password='" + secretPW + "' and firstName='" + owner.getFname() + "' and lastName='" + owner.getLname() +
            "' and email='" + owner.getEmail() + "' and yardage='" + owner.getYardage() + "' and gender='" + owner.getGender() + "'";
  }


  private String getSQLNewTableUsers(){
    return "CREATE TABLE IF NOT EXISTS users (priority varchar(100), username varchar(100), password varchar(100), fname varchar(100), lname varchar(100), email varchar(100), gender varchar(100))";
  }


  private String getSQLInsertUser(User user, String secretPW){
    return "INSERT INTO users (priority, username, password, fname, lname, email, gender) VALUES ('" + user.getPriority() + "','" + user.getUsername() + "','" + secretPW + "','" + user.getFname() + "','" + user.getLname() + "','" + user.getEmail() + "','" + user.getGender() + "')";
  }


  private String convertToSnakeCase(String toConvert){
    String updated = "";
    for(int i = 0; i < toConvert.length(); i++){
      if(toConvert.charAt(i) == ' '){
        updated += '_';
      }else{
        updated += toConvert.charAt(i);
      }
    }
    return updated;
  }


  private String convertFromSnakeCase(String toConvert){
    String updated = "";
    for(int i = 0; i < toConvert.length(); i++){
      if(toConvert.charAt(i) == '_'){
        updated += ' ';
      }else{
        updated += toConvert.charAt(i);
      }
    }
    return updated;
  }


//**********************
// HOME PAGE
//**********************

  @GetMapping(
          path = "/tee-rific/home/{username}"
  )
  public String getHomePage(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) throws Exception{

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/home/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    try(Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      String getUserPriority= "SELECT priority FROM users WHERE username='" + user +"'";
      ResultSet rs = stmt.executeQuery(getUserPriority);

      String userPriority = "";
      while(rs.next()){
        userPriority = rs.getString("priority");
      }

      if(userPriority.equals(priorities[0])){         // returns golfer homepage
        return "LandingPages/home";
      }else if(userPriority.equals(priorities[1])){   //returns owner homepage
        return "Owner/ownerHome";
      }else{                                          //returns admin homepage
        return "Admin/adminHome";
      }
    }catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


//**********************
// COURSES
//**********************

//TODO: get all the courses, display ratings, allow user to rate courses

//**********************
// MODIFY ACCOUNT
//**********************

  @GetMapping(
          path = "/tee-rific/account/{username}"
  )
  public String getAccountPage(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request)
  {
    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/account/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    model.put("username", user);
    return "AccountInfo/account";
  }


  @GetMapping(
          path = "/tee-rific/editAccount/{username}"
  )

  public String getEditAccountPage(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request){

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/editAccount/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    try(Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      String getUserPriority= "SELECT priority FROM users WHERE username='" + user +"'";
      ResultSet rs = stmt.executeQuery(getUserPriority);

      String userPriority = "";
      while(rs.next()){
        userPriority = rs.getString("priority");
      }

      if(userPriority.equals(priorities[0])){         // returns golfer edit account
        //TODO: get the user object and pass the info into the model
        model.put("username", user);
        return "AccountInfo/editAccount";
      }else{                                          //returns owner edit account
        //TODO: get the owner object and pass the info into the model
        model.put("username", user);
        return "AccountInfo/editAccountOwner";
      }
    }catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @PostMapping(
          path = "/tee-rific/editAccount/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String updateAccountInformation(@PathVariable("username")String user, Map<String, Object> model){
    //TODO: add a method to update the desired Account information
    return "redirect:/tee-rific/editAccount/{username}";
  }


  @GetMapping(
          path = "/tee-rific/delete/{username}"
  )
  public String accountDeleted(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request)
  {
    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/delete/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    model.put("username", user);
    return "AccountInfo/AccountDeleted";
  }


  @PostMapping(
          path = "/tee-rific/delete/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String deleteUser(@PathVariable("username")String user, Map<String, Object> model) throws Exception {
    try (Connection connection = dataSource.getConnection())
    {
      Statement stmt = connection.createStatement();

      //if the user Account is an owner, user login information from user table and owner table is deleted, leave course details so scorecards wont be NULL for columns that need them, needs to be tested
      String remove = "DELETE FROM users WHERE username ='" + user + "'";
      stmt.execute(remove);
      remove = "DELETE FROM owners WHERE username ='" + user + "'";
      stmt.execute(remove);

      model.put("username", user);
      return "AccountInfo/AccountDeleted";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


//********************************
// MODIFY COURSE DETAILS -- OWNER
//********************************

  @GetMapping(
          path = "/tee-rific/golfCourseDetails/{username}"
  )
  public String getCourseDetails(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request){

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/golfCourseDetails/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    //TODO: get the course details here and insert into the model

    model.put("username", user);
    return "Booking&ViewingCourses/golfCourseDetails";
  }


//TODO: add a post-method to modify the golf course details


//**********************
// BOOKING
//**********************

  @GetMapping(
          path = "/tee-rific/booking/{username}"
  )
  public String getBookingPage(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request){

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/booking/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    model.put("username", user);
    return "Booking&ViewingCourses/bookingCourse";
  }


  @PostMapping(
          path = "/tee-rific/booking/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String updateSchedule(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) throws Exception {

    try (Connection connection = dataSource.getConnection()) {

      boolean validAppointment = true;
      // Statement stmt = connection.createStatement();

      if(validAppointment){
        // //creates a new scorecard
        // Scorecard scorecard = new Scorecard();

        // //TODO: the argument passed in should be a serial of some sort so that there is never multiple gameID's, then the serial should be assigned to the user in some sort of way so that everyone does not get access to the game
        // scorecard.setGameID(String.valueOf(serial));
        // scorecard.setDatePlayed("");
        // scorecard.setCoursePlayed("My Great Course");
        // scorecard.setTeesPlayed("");
        // scorecard.setHolesPlayed("");
        // scorecard.setFormatPlayed("");
        // scorecard.setAttestor("");
        // serial++;

        // //TODO: Scorecards -- store an arrayList/array in SQL for the users - MIKE
        // //TODO: Scorecards -- how to store an arrayList/array in SQL for the users - MIKE
        // String sqlScorecardsInit = "CREATE TABLE IF NOT EXISTS scorecards (id varchar(100), date varchar(100), course varchar(100), teesPlayed varchar(100), holesPlayed varchar(100), formatPlayed varchar(100), attestor varchar(100))";
        // stmt.executeUpdate(sqlScorecardsInit);

        // stmt.executeUpdate("INSERT INTO scorecards (id, date, course, teesPlayed, holesPlayed, formatPlayed, attestor) VALUES (" +
        //                   "'" + scorecard.getGameID() + "', '" + scorecard.getDatePlayed() + "', '" + scorecard.getCoursePlayed() +
        //                   "', '" + scorecard.getTeesPlayed() + "', '" + scorecard.getHolesPlayed() + "', '" + scorecard.getFormatPlayed() +
        //                   "', '" + scorecard.getAttestor() + "')");

        model.put("username", user);
        return "Booking&ViewingCourses/bookingSuccessful";     //may need to change this
      }
      model.put("username", user);
      return "Booking&ViewingCourses/booking";
    }catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @GetMapping(
          path = "/tee-rific/bookingSuccessful"
  )
  public String bookingSuccessful(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request){

    if(!user.equals(request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    model.put("username", user);
    return "Booking&ViewingCourses/bookingSuccessful";
  }


//**********************
// RENT EQUIPMENT
//**********************

  //TODO: Add extra style
//TODO: Corner cases (out of stock case, quantity in cart > stock case, negative number case)
  @GetMapping(
          path = "/tee-rific/rentEquipment/{username}"
  )
  public String rentEquipment(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) {

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/rentEquipment/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    EquipmentCart cart = new EquipmentCart();

    model.put("username", user);
    model.put("cart", cart);
    return "Rentals/rentEquipment";
  }


  @PostMapping(
          path = "/tee-rific/rentEquipment/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String handleShop(@PathVariable("username")String user, Map<String, Object> model, EquipmentCart cart, HttpServletRequest request) throws Exception {

    try (Connection connection = dataSource.getConnection()) {
      // Create a table with our cart data inside to display on checkout
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS cart (numBalls integer, numCarts integer, numClubs integer)");
      stmt.executeUpdate("INSERT INTO cart VALUES ('"+cart.getNumBalls()+"', '"+cart.getNumCarts()+"', '"+cart.getNumClubs()+"')");
    }

    model.put("username", user);
    return "redirect:/tee-rific/rentEquipment/checkout/" + user;
  }


  @GetMapping(
          path="/tee-rific/rentEquipment/checkout/{username}"
  )
  public String handleViewCart(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) throws Exception {

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/rentEquipment/checkout/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    try (Connection connection = dataSource.getConnection()) {
      // Create a table with our cart data inside to display on checkout
      EquipmentCart toView = getUserCartContentsFromDB(connection);
      toView.printfields();

      model.put("username", user);
      model.put("userCart", toView);
      return "Rentals/rentEquipmentCheckout";
    }
  }


  @PostMapping(
          path = "/tee-rific/rentEquipment/checkout/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String handleCheckout(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) throws Exception {

    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      EquipmentCart cart = getUserCartContentsFromDB(connection);
      updateInventory(connection, cart);
      stmt.executeUpdate("DROP TABLE cart");

      // Create table of rentals so employees can keep track
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS LiveRentals (id serial, username varchar(100), dateCheckout timestamp DEFAULT now(), numBalls integer, numCarts integer, numClubs integer)");
      //TODO: Link user to rental
      stmt.executeUpdate("INSERT INTO LiveRentals (username, numBalls, numCarts, numClubs) VALUES ('temp', '"+cart.getNumBalls()+"', '"+cart.getNumCarts()+"', '"+cart.getNumClubs()+"')");
    }
    model.put("username", user);
    return "redirect:/tee-rific/rentEquipment/checkout/success/" + user;
  }


  @GetMapping(
          path="/tee-rific/rentEquipment/checkout/success/{username}"
  )
  public String rentSuccessPage(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) {

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/rentEquipment/checkout/success/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    model.put("username", user);
    return "Rentals/rentEquipmentSuccess";
  }


  // ------ OWNER'S PAGE ------ //
// TODO: ensure paths are correct
// TODO: no way to secure IF LOGGED IN ALREADY - kyle
// TODO: Add style to table in viewInventory page
  @GetMapping(
          path="/tee-rific/golfCourseDetails/inventory"
  )
  public String viewInventory(Map<String, Object> model, HttpServletRequest request) throws Exception {
    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM inventory");

      ArrayList<Equipment> eqs = new ArrayList<Equipment>();
      while (rs.next()) {
        Equipment eq = new Equipment();
        eq.setItemName(rs.getString("name"));
        eq.setStock(rs.getInt("stock"));

        eqs.add(eq);
      }
      model.put("eqsArray", eqs);
      return "Rentals/inventory";
    }
  }

  // TODO: no way to secure - kyle
// TODO: ensure paths are correct
  @GetMapping(
          path="/tee-rific/golfCourseDetails/inventory/update"
  )
  public String invUpdate(Map<String, Object> model, HttpServletRequest request) {

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    EquipmentCart cart = new EquipmentCart();
    model.put("ownerCart", cart);
    return "Rentals/inventoryUpdate";
  }


  // TODO: ensure paths are correct
  @PostMapping(
          path="/tee-rific/golfCourseDetails/inventory/update",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String handleInvUpdate(Map<String, Object> model, EquipmentCart cart) throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      ownerUpdateInventory(connection, cart);
    }
    return "redirect:/tee-rific/golfCourseDetails/inventory";
  }


  private void updateInventory(Connection connection, EquipmentCart cart) throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM inventory");

    // Calculate updated values for stock
    rs.next();
    int ballStock = rs.getInt("stock");
    int updatedBallStock = ballStock - cart.getNumBalls();
    rs.next();
    int golfCartStock = rs.getInt("stock");
    int updatedGolfCartStock = golfCartStock - cart.getNumCarts();
    rs.next();
    int clubStock = rs.getInt("stock");
    int updatedClubStock = clubStock - cart.getNumClubs();

    // Update inventory table
    stmt.executeUpdate("UPDATE inventory SET stock ='"+updatedBallStock+"' WHERE name = 'balls'");
    stmt.executeUpdate("UPDATE inventory SET stock ='"+updatedGolfCartStock+"' WHERE name = 'carts'");
    stmt.executeUpdate("UPDATE inventory SET stock ='"+updatedClubStock+"' WHERE name = 'clubs'");
  }


  private EquipmentCart getUserCartContentsFromDB(Connection connection) throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM cart");
    rs.next();

    EquipmentCart ret = new EquipmentCart();
    ret.setNumBalls(rs.getInt("numballs"));
    ret.setNumCarts(rs.getInt("numcarts"));
    ret.setNumClubs(rs.getInt("numclubs"));

    return ret;
  }


  private void ownerCreateInventory(Connection connection) throws Exception {
    Statement stmt = connection.createStatement();
    stmt.executeUpdate("CREATE TABLE IF NOT EXISTS inventory (name varchar(100), stock integer DEFAULT 0)");
    stmt.executeUpdate("INSERT INTO inventory (name) VALUES ('balls')");
    stmt.executeUpdate("INSERT INTO inventory (name) VALUES ('carts')");
    stmt.executeUpdate("INSERT INTO inventory (name) VALUES ('clubs')");
  }


  private void ownerInsertNewItem(Connection connection, String nameOfItem) throws Exception {
    Statement stmt = connection.createStatement();
    stmt.executeUpdate("INSERT INTO inventory (name) VALUES ('"+nameOfItem+"')");
  }


  private void ownerDeleteItem(Connection connection, String nameOfItem) throws Exception {
    Statement stmt = connection.createStatement();
    stmt.executeUpdate("DELETE FROM inventory WHERE name='"+nameOfItem+"'");
  }


  private void ownerUpdateInventory(Connection connection, EquipmentCart cart) throws Exception {
    Statement stmt = connection.createStatement();
    // ResultSet rs = stmt.executeQuery("SELECT * FROM inventory");

    // rs.next();
    // int ballStock = rs.getInt("stock");
    // int updatedBallStock = ballStock + cart.getNumBalls();
    // rs.next();
    // int golfCartStock = rs.getInt("stock");
    // int updatedGolfCartStock = golfCartStock + cart.getNumCarts();
    // rs.next();
    // int clubStock = rs.getInt("stock");
    // int updatedClubStock = clubStock + cart.getNumClubs();

    // Update inventory table
    stmt.executeUpdate("UPDATE inventory SET stock ='"+cart.getNumBalls()+"' WHERE name = 'balls'");
    stmt.executeUpdate("UPDATE inventory SET stock ='"+cart.getNumCarts()+"' WHERE name = 'carts'");
    stmt.executeUpdate("UPDATE inventory SET stock ='"+cart.getNumClubs()+"' WHERE name = 'clubs'");
  }


//**********************
// BROWSE COURSES
//**********************

  @GetMapping(
          path = "/tee-rific/courses/{username}"
  )
  public String viewAllCourses(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) throws Exception {
    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/courses/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate(getSQLNewTableOwner());
      ResultSet rs = stmt.executeQuery("SELECT * FROM owners");
      ArrayList<CourseOwner> output = new ArrayList<CourseOwner>();
      while(rs.next()) {
        CourseOwner course = new CourseOwner();
        course.setCourseName(rs.getString("courseName"));
        course.setAddress(rs.getString("address"));
        course.setCity(rs.getString("city"));
        course.setCountry(rs.getString("country"));
        course.setWebsite(rs.getString("website"));
        course.setPhoneNumber(rs.getString("phoneNumber"));
        course.setCourseLogo(rs.getString("courseLogo"));
        course.setDirectionsToCourse(rs.getString("directionsToCourse"));
        course.setDescription(rs.getString("description"));
        course.setWeekdayRates(rs.getString("weekdayRates"));
        course.setWeekendRates(rs.getString("weekendRates"));
        course.setNumHoles(rs.getInt("numHoles"));
        course.setUsername(rs.getString("userName"));
        course.setPassword(rs.getString("password"));
        course.setFname(rs.getString("firstName"));
        course.setLname(rs.getString("lastName"));
        course.setEmail(rs.getString("email"));
        course.setYardage(rs.getString("yardage"));
        course.setGender(rs.getString("gender"));

        output.add(course);
      }
      model.put("courses", output);
      model.put("username", user);
      return "Booking&ViewingCourses/listCourses";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @GetMapping(
          path="/tee-rific/courses/{courseID}/{username}"
  )
  public String getCourseInfo(@PathVariable("courseID")String courseID, @PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) throws Exception {

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    if(!user.equals(request.getSession().getAttribute("username"))) {
      return "redirect:/tee-rific/home" + request.getSession().getAttribute("username");
    }

    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();

      //get the course info by searching for snakeCase name in DB
      String convertedName = convertToSnakeCase(courseID);

      String getCourseInfo = "SELECT * FROM " + convertedName;
      ResultSet courseInfo = stmt.executeQuery(getCourseInfo);
      ArrayList<Hole> courseHoles = new ArrayList<Hole>();
      while(courseInfo.next()){
        Hole hole = new Hole();
        hole.setHoleNumber(Integer.parseInt(courseInfo.getString("holeNumber")));
        hole.setYardage(Integer.parseInt(courseInfo.getString("yardage")));
        hole.setPar(Integer.parseInt(courseInfo.getString("par")));
        hole.setHandicap(Integer.parseInt(courseInfo.getString("handicap")));

        courseHoles.add(hole);
      }

      model.put("courseName", courseID);
      model.put("username", user);
      model.put("course", courseHoles);

      return "Booking&ViewingCourses/courseInformation";
    }catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


//**********************
// SCORECARD
//**********************

  @GetMapping(
          path = "/tee-rific/scorecards/{username}"
  )
  public String getScorecards(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request) throws Exception {

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/scorecards/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();

      //TODO: Scorecards -- how to store an arrayList/array in SQL for the users
      String sqlScorecardsInit = "CREATE TABLE IF NOT EXISTS scorecards (id varchar(100), date varchar(100), course varchar(100), teesPlayed varchar(100), holesPlayed varchar(100), formatPlayed varchar(100), attestor varchar(100))";
      stmt.executeUpdate(sqlScorecardsInit);

      ResultSet rs = stmt.executeQuery("SELECT * FROM scorecards");
      ArrayList<Scorecard> output = new ArrayList<Scorecard>();
      while(rs.next()) {
        Scorecard scorecard = new Scorecard();
        scorecard.setGameID(rs.getString("id"));
        scorecard.setDatePlayed(rs.getString("date"));
        scorecard.setCoursePlayed(rs.getString("course"));
        scorecard.setTeesPlayed(rs.getString("teesPlayed"));
        scorecard.setHolesPlayed(rs.getString("holesPlayed"));
        scorecard.setFormatPlayed(rs.getString("formatPlayed"));
        scorecard.setAttestor(rs.getString("attestor"));
        //TODO: Scorecards -- store an arrayList/array in SQL for the users

        output.add(scorecard);
      }

      model.put("username", user);
      model.put("scorecards", output);
      return "Scorecard/scorecard";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @GetMapping(
          path = "/tee-rific/scorecards/{username}/{gameID}"
  )
  public String getSpecificScorecard(@PathVariable("username")String user, @PathVariable("gameID")String gameID, Map<String, Object> model, HttpServletRequest request) throws Exception {

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    if(!user.equals(request.getSession().getAttribute("username"))) {
      return "redirect:/tee-rific/home/" + request.getSession().getAttribute("username");
    }

    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();

      //get the scorecard info
      String getScorecardInfo = "SELECT * FROM scorecards WHERE id='" + gameID + "'";
      ResultSet scoreCardInfo = stmt.executeQuery(getScorecardInfo);

      Scorecard scorecard = new Scorecard();
      while(scoreCardInfo.next()){
        scorecard.setGameID(scoreCardInfo.getString("id"));
        scorecard.setDatePlayed(scoreCardInfo.getString("date"));
        scorecard.setCoursePlayed(scoreCardInfo.getString("course"));
        scorecard.setTeesPlayed(scoreCardInfo.getString("teesPlayed"));
        scorecard.setHolesPlayed(scoreCardInfo.getString("holesPlayed"));
        scorecard.setFormatPlayed(scoreCardInfo.getString("formatPlayed"));
        scorecard.setAttestor(scoreCardInfo.getString("attestor"));
        //TODO: Scorecards -- store an arrayList/array in SQL for the users
      }

      //get the course info by searching for snakeCase name in DB
      String courseName = scorecard.getCoursePlayed();
      String convertedName = convertToSnakeCase(courseName);

      String getCourseInfo = "SELECT * FROM " + convertedName;
      ResultSet courseInfo = stmt.executeQuery(getCourseInfo);
      ArrayList<Hole> courseHoles = new ArrayList<Hole>();
      while(courseInfo.next()){
        Hole hole = new Hole();
        hole.setHoleNumber(Integer.parseInt(courseInfo.getString("holeNumber")));
        hole.setYardage(Integer.parseInt(courseInfo.getString("yardage")));
        hole.setPar(Integer.parseInt(courseInfo.getString("par")));
        hole.setHandicap(Integer.parseInt(courseInfo.getString("handicap")));

        courseHoles.add(hole);
      }

      model.put("username", user);
      model.put("scorecard", scorecard);
      model.put("course", courseHoles);

      return "Scorecard/game";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


//TODO: postMapping for scorecard updating
// @PostMapping(
//   path = "/tee-rific/scorecards/{gameID}",
//   consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
// )
// public String updateScorecard(@PathVariable("gameID")String gameID, Map<String, Object> model){
//   return "game";
// }//updateScorecard()


//**********************
// TOURNAMENT
//**********************

  @GetMapping(
          path = "/tee-rific/tournament/{username}"
  )
  public String tournament(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request){

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/tournament/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    model.put("username", user);
    return "Tournaments/tournament";
  }

  // TODO: page crashes on the live version, local host works fine for some reason
// TODO: breaks if no tournaments have been created yet
  @GetMapping(
          path = "/tee-rific/availableTournaments/{username}"
  )
  public String availableTournaments(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request){

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/availableTournaments/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    try(Connection connection = dataSource.getConnection())
    {
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM tournaments");
      ArrayList<Tournament> output = new ArrayList<Tournament>();
      while (rs.next())
      {
        Tournament tournament = new Tournament();
        tournament.setId(rs.getInt("id"));
        tournament.setName(rs.getString("name"));
        tournament.setDate(rs.getString("date"));
        tournament.setTime(rs.getString("time"));
        tournament.setParticipantSlots(rs.getInt("participant_slots"));
        tournament.setBuyIn(rs.getInt("buy_in"));
        tournament.setFirstPrize(rs.getString("first_prize"));
        tournament.setSecondPrize(rs.getString("second_prize"));
        tournament.setThirdPrize(rs.getString("third_prize"));
        tournament.setAgeRequirement(rs.getString("age_requirement"));
        tournament.setGameMode(rs.getString("game_mode"));
        tournament.setClubName(rs.getString("club_name"));

        output.add(tournament);
      }

      model.put("tournaments", output);
      Tournament tournament = new Tournament();
      model.put("tournament", tournament);

      model.put("username", user);
      return "Tournaments/availableTournaments";
    } catch (Exception e){
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @GetMapping(
          path = "/tee-rific/createTournament/{username}"
  )
  public String createTournament(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request)
  {

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/createTournament/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    Tournament tournament = new Tournament();

    model.put("newTournament", tournament);
    model.put("username", user);
    return "Tournaments/createTournament";
  }


  @PostMapping(
          path = "/tee-rific/createTournament/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String handleTournamentCreation(@PathVariable("username")String user, Map<String, Object> model, Tournament tournament) throws Exception
  {
    try (Connection connection = dataSource.getConnection())
    {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tournaments (id serial, name varchar(100), date varchar(10), time varchar(50), participant_slots integer, buy_in integer, first_prize varchar(100), second_prize varchar(100), third_prize varchar(100), age_requirement varchar(20), game_mode varchar(100), club_name varchar(100))");
      Integer buyIn = tournament.getBuyIn();
      if (buyIn == null)
      {
        buyIn = 0;
      }
      String firstPrize = tournament.getFirstPrize();
      if (tournament.getFirstPrize() == null)
      {
        firstPrize = "0";
      }
      String secondPrize = tournament.getSecondPrize();
      if (tournament.getSecondPrize() == null)
      {
        secondPrize = "0";
      }
      String thirdPrize = tournament.getThirdPrize();
      if (tournament.getThirdPrize() == null)
      {
        thirdPrize = "0";
      }
      stmt.executeUpdate("INSERT INTO tournaments (name, date, time, participant_slots, buy_in, first_prize, second_prize, third_prize, age_requirement, game_mode, club_name) VALUES ('" + tournament.getName() + "','" + tournament.getDate() + "','" + tournament.getTime() + "','" + tournament.getParticipantSlots() + "','" + buyIn + "','" + firstPrize + "','" + secondPrize + "','" + thirdPrize + "','" + tournament.getAgeRequirement() + "','" + tournament.getGameMode() + "','" + tournament.getClubName() + "')");

      return "redirect:/tee-rific/availableTournaments/" + user;
      // String ageRequirement = tournament.getAgeRequirement();
      // if (tournament.getAgeRequirement() == null)
      // {
      //   ageRequirement = "0";
      // }
      // stmt.executeUpdate("INSERT INTO tournaments (name, date, time, participant_slots, buy_in, first_prize, second_prize, third_prize, age_requirement, game_mode, club_name) VALUES ('" + tournament.getName() + "','" + tournament.getDate() + "','" + tournament.getTime() + "','" + tournament.getParticipantSlots() + "','" + buyIn + "','" + firstPrize + "','" + secondPrize + "','" + thirdPrize + "','" + ageRequirement + "','" + tournament.getGameMode() + "','" + tournament.getClubName() + "')");
      // return "redirect:/tee-rific/availableTournaments";
    } catch (Exception e)
    {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @GetMapping(
          path = "/tee-rific/viewTournament/{tid}/{username}"
  )
  public String viewSelectedTournament(@PathVariable("username")String user, Map<String, Object> model, @PathVariable String tid, HttpServletRequest request)
  {
    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    if(!user.equals(request.getSession().getAttribute("username"))) {
      return "redirect:/tee-rific/home/" + request.getSession().getAttribute("username");
    }

    try(Connection connection = dataSource.getConnection())
    {
      Statement stmt = connection.createStatement();
      model.put("id", tid);
      ResultSet rs = stmt.executeQuery("SELECT * FROM tournaments WHERE id =" + tid);
      ArrayList<Tournament> output = new ArrayList<Tournament>();
      while (rs.next())
      {
        Tournament tournament = new Tournament();
        tournament.setId(rs.getInt("id"));
        tournament.setName(rs.getString("name"));
        tournament.setDate(rs.getString("date"));
        tournament.setTime(rs.getString("time"));
        tournament.setParticipantSlots(rs.getInt("participant_slots"));
        tournament.setBuyIn(rs.getInt("buy_in"));
        tournament.setFirstPrize(rs.getString("first_prize"));
        tournament.setSecondPrize(rs.getString("second_prize"));
        tournament.setThirdPrize(rs.getString("third_prize"));
        tournament.setAgeRequirement(rs.getString("age_requirement"));
        tournament.setGameMode(rs.getString("game_mode"));
        tournament.setClubName(rs.getString("club_name"));

        output.add(tournament);
      }

      model.put("tournaments", output);
      Tournament tournament = new Tournament();
      model.put("tournament", tournament);
      model.put("username", user);
      return "Tournaments/viewTournament";

    } catch (Exception e)
    {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @GetMapping(
          path = "/tee-rific/tournamentDelete/{username}"
  )
  public String displayDeleteTournamentPage(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request)
  {

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/tournamentDelete/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    model.put("username", user);
    return "Tournaments/tournamentDelete";
  }//displayDeleteTournamentPage()


  @PostMapping(
          path = "/tee-rific/tournamentDelete/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String deleteTournament(@PathVariable("username")String user, Map<String, Object> model, Tournament tournament)
  {
    try (Connection connection = dataSource.getConnection())
    {
      Statement stmt = connection.createStatement();
      stmt.execute("DELETE FROM tournaments WHERE id = " + tournament.getId());
      System.out.println(tournament.getId());
      System.out.println(tournament.getName());
      return "redirect:/tee-rific/availableTournaments/" + user;
    } catch (Exception e)
    {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @GetMapping(
          path = "/tee-rific/tournamentSignUp/{username}"
  )
  public String tournamentSignUp(@PathVariable("username")String user, Map<String, Object> model, Tournament tournament, HttpServletRequest request)
  {

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/tournamentSignUp/" + request.getSession().getAttribute("username");
    }

    if(null == (request.getSession().getAttribute("username"))) {
      return "redirect:/";
    }

    try (Connection connection = dataSource.getConnection())
    {
      Statement stmt = connection.createStatement();
      //add user to tournament.participants

      model.put("username", user);
      //pop up displays if the user is already signed up in the tournament
      return "Tournaments/tournamentSignUp";
    } catch (Exception e)
    {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


// @PostMapping(
//   path ="tee-rific/tournamentSignUp",
//   consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
// )
// public String handleTournamentSignUp(Map<String, Object> model, Tournament tournament)
// {
//   try (Connection connection = dataSource.getConnection())
//   {
//     //sign the user up, add them to the participant list
//   } catch (Exception e)
//   {
//     model.put("message", e.getMessage());
//     return "error";
//   }
// }


//**********************
// ADMIN BUTTONS
//**********************

  // LIST OF USERS
//--------------------------------
  @GetMapping(
          path = "/tee-rific/admin/users"
  )
  public String listUsers(Map<String, Object> model, HttpServletRequest request)
  {

    if(request.getSession().getAttribute("username") == (null)) {
      return "redirect:/";
    }

    if(!"admin".equals(request.getSession().getAttribute("username"))) {
      return "redirect:/tee-rific/home/" + request.getSession().getAttribute("username");
    }

    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (priority varchar(30), username varchar(30), password varchar(100), fname varchar(30), lname varchar(30), email varchar(30), gender varchar(30))");

      // creates and check if admin account created
      int checkIfAdminExists = 0;
      ResultSet checkAdmin = stmt.executeQuery("SELECT * FROM users WHERE username = 'admin'");
      while (checkAdmin.next()){
        checkIfAdminExists++;
      }
      if (checkIfAdminExists == 0){
        String adminPassword = "cmpt276";
        String encryptedAdminPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt());
        String insert = "INSERT INTO users (priority, username, password) VALUES ('ADMIN','admin','"+encryptedAdminPassword+"')";
        stmt.executeUpdate(insert);
      }

      ResultSet listU = stmt.executeQuery("SELECT * FROM users");
      ArrayList<User> output = new ArrayList<User>();
      while (listU.next()) {
        User temp = new User();

        temp.setPriority(listU.getString("priority"));
        temp.setUsername(listU.getString("username"));
        temp.setPassword(listU.getString("password"));
        temp.setFname(listU.getString("fname"));
        temp.setLname(listU.getString("lname"));
        temp.setEmail(listU.getString("email"));
        temp.setGender(listU.getString("gender"));

        output.add(temp);
      }

      model.put("userList",output);
      return "Admin/listOfUsers";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

  @PostMapping(
          path = "/tee-rific/admin/users/clear",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String clearUserDB(Map<String, Object> model){
    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      String sql = "DROP TABLE users";
      stmt.executeUpdate(sql);

      return "redirect:/tee-rific/admin/users";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }


  @PostMapping(
          path = "/tee-rific/admin/users/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String deleteUser(Map<String, Object> model, @PathVariable("username") String name){
    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      String sql = "DELETE FROM users WHERE username='"+name+"'";
      stmt.executeUpdate(sql);

      return "LandingPages/deleteSuccess";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

  // LIST OF TOURNAMENTS
//--------------------------------
  @GetMapping(
          path = "/tee-rific/admin/tournaments"
  )
  public String listTournaments(Map<String, Object> model, HttpServletRequest request)
  {

    if(request.getSession().getAttribute("username") == (null)) {
      return "redirect:/";
    }

    if(!"admin".equals(request.getSession().getAttribute("username"))) {
      return "redirect:/tee-rific/home/" + request.getSession().getAttribute("username");
    }
    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tournaments (id serial, name varchar(50), participant_slots integer, buy_in integer, first_prize varchar(30), second_prize varchar(30), third_prize varchar(30), age_requirement integer, game_mode varchar(30), club_name varchar(50))");
      ResultSet listT = stmt.executeQuery("SELECT * FROM tournaments IF ");
      ArrayList<Tournament> output = new ArrayList<Tournament>();
      while (listT.next()) {
        Tournament temp = new Tournament();

        temp.setId(listT.getInt("id"));
        temp.setName(listT.getString("name"));
        temp.setDate(listT.getString("date"));
        temp.setTime(listT.getString("time"));
        temp.setParticipantSlots(listT.getInt("participant_slots"));
        temp.setBuyIn(listT.getInt("buy_in"));
        temp.setFirstPrize(listT.getString("first_prize"));
        temp.setSecondPrize(listT.getString("second_prize"));
        temp.setThirdPrize(listT.getString("third_prize"));
        temp.setAgeRequirement(listT.getString("age_requirement"));
        temp.setGameMode(listT.getString("game_mode"));
        temp.setClubName(listT.getString("club_name"));

        output.add(temp);
      }

      model.put("tournamentList",output);
      return "Tournaments/listOfTournaments";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

  @PostMapping(
          path = "/tee-rific/admin/tournaments/clear",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String clearTournamentDB(Map<String, Object> model){
    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      String sql = "DROP TABLE tournaments";
      stmt.executeUpdate(sql);

      return "redirect:/tee-rific/admin/tournaments";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

  @PostMapping(
          path = "/tee-rific/admin/tournaments/{id}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String deleteTournament(Map<String, Object> model, @PathVariable("id") long id){
    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      String sql = "DELETE FROM tournaments WHERE id="+id;
      stmt.executeUpdate(sql);

      return "LandingPages/deleteSuccess";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

  // LIST OF OWNERS AND GOLF COURSES
//--------------------------------
  @GetMapping(
          path = "/tee-rific/admin/owners"
  )
  public String listOwners(Map<String, Object> model, HttpServletRequest request)
  {

    if(request.getSession().getAttribute("username") == (null)) {
      return "redirect:/";
    }

    if(!"admin".equals(request.getSession().getAttribute("username"))) {
      return "redirect:/tee-rific/home/" + request.getSession().getAttribute("username");
    }

    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      String checkIfOwnerTExists = getSQLNewTableOwner();
      stmt.executeUpdate(checkIfOwnerTExists);
      ResultSet listO = stmt.executeQuery("SELECT * FROM owners");
      ArrayList<CourseOwner> output = new ArrayList<CourseOwner>();
      while (listO.next()) {
        CourseOwner temp = new CourseOwner();

        temp.setCourseName(listO.getString("coursename"));
        temp.setAddress(listO.getString("address"));
        temp.setCity(listO.getString("city"));
        temp.setCountry(listO.getString("country"));

        temp.setUsername(listO.getString("username"));
        temp.setPassword(listO.getString("password"));
        temp.setFname(listO.getString("firstname"));
        temp.setLname(listO.getString("lastname"));
        temp.setEmail(listO.getString("email"));
        temp.setGender(listO.getString("gender"));

        output.add(temp);
      }

      model.put("ownerList",output);
      return "Admin/listOfOwners";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

  @PostMapping(
          path = "/tee-rific/admin/owners/clear",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String clearOwnerDB(Map<String, Object> model){
    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      String sql = "DROP TABLE users";
      stmt.executeUpdate(sql);

      return "redirect:/tee-rific/admin/owners";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

  @PostMapping(
          path = "/tee-rific/admin/owner/{username}",
          consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public String deleteOwner(Map<String, Object> model, @PathVariable("username") String name){
    try (Connection connection = dataSource.getConnection()){
      Statement stmt = connection.createStatement();
      String sql = "DELETE FROM owners WHERE username='"+name+"'";
      stmt.executeUpdate(sql);
      String userSql = "DELETE FROM users WHERE username='"+name+"'";
      stmt.executeUpdate(userSql);

      return "LandingPages/deleteSuccess";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

  @GetMapping(
          path = "/tee-rific/admin/owners/golfCourse/{courseName}"
  )
  public String viewGolfCourse(Map<String, Object> model, @PathVariable("courseName") String course, HttpServletRequest request){

    if(request.getSession().getAttribute("username") == (null)) {
      return "redirect:/";
    }

    if(!"admin".equals(request.getSession().getAttribute("username"))) {
      return "redirect:/tee-rific/home/" + request.getSession().getAttribute("username");
    }

    try (Connection connection = dataSource.getConnection()){
      String searchCourse = convertToSnakeCase(course);

      Statement stmt = connection.createStatement();
      String sql = "SELECT * FROM "+searchCourse;
      ResultSet courseDetails = stmt.executeQuery(sql);

      ArrayList<Hole> output = new ArrayList<Hole>();
      while (courseDetails.next()) {
        Hole temp = new Hole();

        //(holeNumber integer, yardage integer, par integer, handicap integer)
        temp.setHoleNumber(courseDetails.getInt("holeNumber"));
        temp.setYardage(courseDetails.getInt("yardage"));
        temp.setPar(courseDetails.getInt("par"));
        temp.setHandicap(courseDetails.getInt("handicap"));

        output.add(temp);
      }

      model.put("details",output);
      return "Admin/courseDetails";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "LandingPages/error";
    }
  }

//**********************
// ABOUT-US
//**********************

  @GetMapping(
          path = "/tee-rific/aboutUs/{username}"
  )
  public String aboutDevelopers(@PathVariable("username")String user, Map<String, Object> model, HttpServletRequest request){

    if(!user.equals(request.getSession().getAttribute("username")) && (request.getSession().getAttribute("username") != (null))) {
      return "redirect:/tee-rific/aboutUs/" + request.getSession().getAttribute("username");
    }

    if(request.getSession().getAttribute("username") == (null)) {
      return "redirect:/";
    }

    //this is optional, if you guys feel comfortable doing so, we can upload 'selfies' of our team and maybe talk about our development process
    model.put("username", user);
    return "LandingPages/aboutUs";
  }


//**********************
// LOGOUT
//**********************

  @GetMapping(
          path = "/tee-rific/logout"
  )
  public String Logout(HttpServletRequest request){

    if(request.getSession().getAttribute("username") == (null)) {
      return "redirect:/";
    }

    //log the user out
    return "LandingPages/logout";
  }

  @PostMapping(
          path = "tee-rific/logout"
  )
  public String confirmLogout(HttpServletRequest request) {
    request.getSession().invalidate();
    return "redirect:/";
  }

}