package db_package;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import db_package.StandardInputRead;
import java.util.Scanner;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.util.Vector;
import java.sql.PreparedStatement;

public class DbApp {
	
	private Connection conn;
	Vector<Savepoint> sp;
	static int TransactionsNum; 

	
	public  DbApp (String IPaddress, String dbName, String username, String password) {
		String s= IPaddress+"/"+dbName; //concatenate IPaddress and dbName because getConnection has 3 string variables as arguments
		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection(s, username, password);
			System.out.println("Connection is successfull!\n");
			conn.setAutoCommit(false); //to execute more than one sql command in a transaction
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public void startTransactions() {
		try {
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void commit() {
		try {
			conn.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void abort() {
		try {
			conn.rollback();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/*public void waitForEnter() {
		Scanner scn = new Scanner(System.in);
		System.out.println("Press Enter ...");
		scn.nextLine();
	}*/
	
	public void showRegisteredStudents(short typ_year, String typ_season, String courseCode) {
		try {
			CallableStatement cst = conn.prepareCall("{call displayRegisteredStudents8(?,?,?)}");//function displayRegisteredStudents8 exists in our database
			cst.setShort(1,typ_year);  
			cst.setString(2,typ_season); 
			cst.setString(3,courseCode); 
			cst.execute();  
			ResultSet res = cst.executeQuery();
			while (res.next()) {
				System.out.println("amka:"+res.getInt(1)+"\t" +" name:"+res.getString(2)+"surname:"+ res.getString(3));
			}
			
			res.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}	
	}
	
	public void showGrades(short typ_year, String typ_season, int student_amka) {
		try {
			CallableStatement cst = conn.prepareCall("{call show_grades1(?,?,?)}"); //function show_grades1 exists in our database
			cst.setShort(1,typ_year);  
			cst.setString(2,typ_season); 
			cst.setInt(3,student_amka); 
			cst.execute();  
			ResultSet res = cst.executeQuery();
			int counter=0; //for ascending number before every result
			while (res.next()) {
				counter ++;
				System.out.println(counter + " : \t"+ "course_code:"+res.getString(1)+"\t" +" course_title:"+res.getString(2)+"lab_grade:"+ res.getBigDecimal(3)+"\t"+"exam_grade:"+ res.getBigDecimal(4));
			}
			int c=counter; //help counter because variable counter is going to be set as 0 again
			TransactionsNum=0; //make TransactionsNum=0 every time the user selects option 5
			this.sp = new Vector<>(); //vector to hold savepoints
			while (true) {
			Scanner sc = new Scanner(System.in);
	        System.out.print("Enter a number to change lab and exam grade: \n ");
	        int num = sc.nextInt();
			if (num==0) { //if num=0 return to main menu
				break;
				}
			else if (num==-1) { //if num=-1 abort last transaction
				this.abortTransaction();
			}
			else if(num > 0){
				if (num>c)  //if user wants to replace grades for a course that does not exist for the given student amka 
					System.out.println("Cannot replace grade for that number. Try again \n");
				else
				{
				counter=0;
				TransactionsNum++; //every time the user makes a transaction increase the number by 1
				System.out.println("Number of Transactions remaining : \t"+TransactionsNum);
				res = cst.executeQuery(); //to execute again previous query
				while (res.next()) {
				counter ++; 
				if (counter==num) { //wait until we reach the course that user wants to change grades for
				System.out.println("Insert lab grade :\n");
				BigDecimal lab_grade = sc.nextBigDecimal(); //numeric in sql= BigDecimal in java
				System.out.println("Insert exam grade :\n");
				BigDecimal exam_grade = sc.nextBigDecimal();
				PreparedStatement updateQuery = conn.prepareStatement("UPDATE \"Register\"\r\n" + 
						"SET lab_grade=?, exam_grade=?\r\n" + 
						"WHERE course_code=? and amka=?");
                updateQuery.setBigDecimal(1,lab_grade);
                updateQuery.setBigDecimal(2,exam_grade);
                updateQuery.setString(3,res.getString(1) ); //res.getString(1)=course_code
                updateQuery.setInt(4,student_amka); //amka=student_amka
                updateQuery.executeUpdate();
                this.sp.add(conn.setSavepoint()); //add this savepoint to the vector of savepoints
                break;
				}
				}
				}
			}
			}
			res.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}	
	}
	public void abort (Savepoint savepoint) { //abort on a savepoint
		try {
			conn.rollback(savepoint);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void abortTransaction() {
		System.out.println("Number of Transactions remaining : \t"+TransactionsNum);
		if (TransactionsNum > 1) { //if there are many transactions
				this.abort(this.sp.get(0)); //abort the first element of the vector with savepoints
				TransactionsNum--; //decrease number of transactions by 1
				System.out.println("Transaction aborted \n");
				System.out.println("Number of Transactions remaining : \t"+TransactionsNum);
		} 
		else if (TransactionsNum == 1) { //if there is only one transaction
			this.abort(); //abort it by aborting everything
			TransactionsNum--; //decrease number of transactions by 1
			System.out.println("Transaction aborted \n");
			System.out.println("Number of Transactions remaining : \t"+TransactionsNum);
		}
		else { //if there are no remaining transactions
	        System.out.println("Cannot abort transaction \n");
			return;
		}
	}
	

	public static void main(String[] args) {
		DbApp da = null;
		int userOption = 0;
		 StandardInputRead reader = new StandardInputRead();
		while (userOption!=6){
			 printMenu();
	         String userInput = reader.readString("Insert a number to make a choice \n ");
	            if (userInput == null) {
	                continue;
	            } else {
	                try {
	                    userOption = Integer.parseInt(userInput);
	                } catch (NumberFormatException ex) {
	                    userOption = 0;
	                }
	            }	            
	            switch (userOption) {
	                case 0:
	                    continue;
	                case 1:	//connect to jdbc
	                	String IPaddress=reader.readString("Insert IP Address \n"); //jdbc:postgresql://localhost:5432
	                	String dbName=reader.readString("Insert DataBase Name \n");
	                	String username=reader.readString("Insert username \n");
	                	String password=reader.readString("Insert password \n");
	                	da=new DbApp(IPaddress,dbName,username,password);	             
	                    break;
	                case 2: // Commit/start new
	                	if (da!=null) {
	                		da.commit();
	                		System.out.println("Transaction Committed \n");
	                	}
	                	else 
	                		System.out.println("DbApp not found \n");
	                		//da.startTransactions();
	                    break;
	                case 3: //Abort/start new
	                	if (da!=null) {
	                		da.abort();
	                		System.out.println("Transaction Aborted \n");
	                	}
	                	else 
	                		System.out.println("DbApp not found \n");
	                		//da.startTransactions();
	                    break;
	                case 4: // Show registered students
	                	if (da!=null) {
		                	short typ_year=reader.readShort("Insert Typical Year \n");
		                	String typ_season=reader.readString("Insert Typical Season \n");
		                	String courseCode=reader.readString("Insert Course Code \n");
		                	da.showRegisteredStudents(typ_year,typ_season,courseCode);
	                	}
	                	else
	                		System.out.println("DbApp not found \n");	                	
	                    break;
	                case 5: // Show grade
	                	if (da!=null) {
		                	short typYear=reader.readShort("Insert Typical Year \n");
		                	String typSeason=reader.readString("Insert Typical Season \n");
		                	int studentAmka=reader.readPositiveInt("Insert Student Amka \n");
		                	da.showGrades(typYear,typSeason,studentAmka);
	                	}
	                	else
	                		System.out.println("DbApp not found \n");	
		                break;
	                case 6: // Exit
	                	System.out.println("Exit \n");
	                    break;
	                default:	                    
	                    System.out.println("User option " + userOption + " ignored...");
	                    continue;
	            }
		}
		
	}		
	
	
	 public static void printMenu() {
	        System.out.println("Menu\n");
	        System.out.println("1->Insert data to connect to jdbc \n");
	        System.out.println("2->Commit Transaction/ Start new Transaction \n");
	        System.out.println("3->Abort Transaction/ Start new Transaction \n");  
	        System.out.println("4->Show registered students \n"); 
	        System.out.println("5->Show grade \n");
	        System.out.println("6->Exit \n");
	        System.out.println("=======================================================");   
	    }

}
