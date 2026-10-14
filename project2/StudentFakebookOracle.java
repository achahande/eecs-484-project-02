package project2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;

/*
    The StudentFakebookOracle class is derived from the FakebookOracle class and implements
    the abstract query functions that investigate the database provided via the <connection>
    parameter of the constructor to discover specific information.
*/
public final class StudentFakebookOracle extends FakebookOracle {
    // [Constructor]
    // REQUIRES: <connection> is a valid JDBC connection
    public StudentFakebookOracle(Connection connection) {
        oracle = connection;
    }

    @Override
    // Query 0
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the total number of users for which a birth month is listed
    //        (B) Find the birth month in which the most users were born
    //        (C) Find the birth month in which the fewest users (at least one) were born
    //        (D) Find the IDs, first names, and last names of users born in the month
    //            identified in (B)
    //        (E) Find the IDs, first names, and last name of users born in the month
    //            identified in (C)
    //
    // This query is provided to you completed for reference. Below you will find the appropriate
    // mechanisms for opening up a statement, executing a query, walking through results, extracting
    // data, and more things that you will need to do for the remaining nine queries
    public BirthMonthInfo findMonthOfBirthInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            // Step 1
            // ------------
            // * Find the total number of users with birth month info
            // * Find the month in which the most users were born
            // * Find the month in which the fewest (but at least 1) users were born
            ResultSet rst = stmt.executeQuery(
                    "SELECT COUNT(*) AS Birthed, Month_of_Birth " + // select birth months and number of uses with that birth month
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth IS NOT NULL " + // for which a birth month is available
                            "GROUP BY Month_of_Birth " + // group into buckets by birth month
                            "ORDER BY Birthed DESC, Month_of_Birth ASC"); // sort by users born in that month, descending; break ties by birth month

            int mostMonth = 0;
            int leastMonth = 0;
            int total = 0;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    mostMonth = rst.getInt(2); //   it is the month with the most
                }
                if (rst.isLast()) { // if last record
                    leastMonth = rst.getInt(2); //   it is the month with the least
                }
                total += rst.getInt(1); // get the first field's value as an integer
            }
            BirthMonthInfo info = new BirthMonthInfo(total, mostMonth, leastMonth);

            // Step 2
            // ------------
            // * Get the names of users born in the most popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + mostMonth + " " + // born in the most popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addMostPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 3
            // ------------
            // * Get the names of users born in the least popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + leastMonth + " " + // born in the least popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addLeastPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 4
            // ------------
            // * Close resources being used
            rst.close();
            stmt.close(); // if you close the statement first, the result set gets closed automatically

            return info;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new BirthMonthInfo(-1, -1, -1);
        }
    }

    @Override
    // Query 1
    // -----------------------------------------------------------------------------------
    // GOALS: (A) The first name(s) with the most letters
    //        (B) The first name(s) with the fewest letters
    //        (C) The first name held by the most users
    //        (D) The number of users whose first name is that identified in (C)
    public FirstNameInfo findNameInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                FirstNameInfo info = new FirstNameInfo();
                info.addLongName("Aristophanes");
                info.addLongName("Michelangelo");
                info.addLongName("Peisistratos");
                info.addShortName("Bob");
                info.addShortName("Sue");
                info.addCommonName("Harold");
                info.addCommonName("Jessica");
                info.setCommonNameCount(42);
                return info;
            */
            FirstNameInfo info = new FirstNameInfo();

            ResultSet rst = stmt.executeQuery(
                "SELECT DISTINCT First_Name " + 
                "FROM " + UsersTable + " " + 
                "WHERE First_Name IS NOT NULL " + 
                "AND LENGTH(First_Name) = (" + 
                    "SELECT MAX(LENGTH(First_Name)) " + 
                    "FROM " + UsersTable + 
                ") " + 
                "ORDER BY First_Name ASC"
            );
            while (rst.next()) {
                info.addLongName(rst.getString(1));
            }
            rst.close();

            rst = stmt.executeQuery(
                "SELECT DISTINCT First_Name " + 
                "FROM " + UsersTable + " " + 
                "WHERE First_Name IS NOT NULL " + 
                "AND LENGTH(First_Name) = (" + 
                    "SELECT MIN(LENGTH(First_Name)) " + 
                    "FROM " + UsersTable + 
                ") " + 
                "ORDER BY First_Name ASC"
            );
            while (rst.next()) {
                info.addShortName(rst.getString(1));
            }
            rst.close();

            rst = stmt.executeQuery(
                "SELECT First_Name, COUNT(*) AS Name_Count " + 
                "FROM " + UsersTable + " " + 
                "WHERE First_Name IS NOT NULL " + 
                "GROUP BY First_Name " + 
                "HAVING COUNT(*) = (" + 
                    "SELECT MAX(Name_Count) " + 
                    "FROM (" + 
                        "SELECT COUNT(*) AS Name_Count " + 
                        "FROM " + UsersTable + " " + 
                        "WHERE First_Name IS NOT NULL " + 
                        "GROUP BY First_Name" + 
                    ") name_counts" + 
                ") " + 
                "ORDER BY First_Name ASC"
            );
            while (rst.next()) {
                info.addCommonName(rst.getString(1));
                info.setCommonNameCount(rst.getLong(2));
            }
            rst.close();

            return info;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new FirstNameInfo();
        }
    }

    @Override
    // Query 2
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users without any friends
    //
    // Be careful! Remember that if two users are friends, the Friends table only contains
    // the one entry (U1, U2) where U1 < U2.
    public FakebookArrayList<UserInfo> lonelyUsers() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(15, "Abraham", "Lincoln");
                UserInfo u2 = new UserInfo(39, "Margaret", "Thatcher");
                results.add(u1);
                results.add(u2);
            */
            ResultSet rst = stmt.executeQuery(
                "SELECT u.User_ID, u.First_Name, u.Last_Name " + 
                "FROM " + UsersTable + " u " + 
                "WHERE NOT EXISTS (" +
                    "SELECT 1 " + 
                    "FROM " + FriendsTable + " f " +
                    "WHERE f.User1_ID = u.User_ID " + 
                    "OR f.User2_ID = u.User_ID " + 
                ") " + 
                "ORDER BY u.User_ID ASC"
            );
            while(rst.next()) {
                results.add(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }
            rst.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 3
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users who no longer live
    //            in their hometown (i.e. their current city and their hometown are different)
    public FakebookArrayList<UserInfo> liveAwayFromHome() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(9, "Meryl", "Streep");
                UserInfo u2 = new UserInfo(104, "Tom", "Hanks");
                results.add(u1);
                results.add(u2);
            */
            ResultSet rst = stmt.executeQuery(
                "SELECT u.User_ID, u.First_Name, u.Last_Name " + 
                "FROM " + UsersTable + " u " + 
                "JOIN " + CurrentCitiesTable + " cc ON u.User_ID = cc.User_ID " + 
                "JOIN " + HometownCitiesTable + " hc ON u.User_ID = hc.User_ID " + 
                "WHERE cc.Current_City_ID <> hc.Hometown_City_ID " +
                "ORDER BY u.User_ID ASC"
            );
            while(rst.next()) {
                results.add(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 4
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, links, and IDs and names of the containing album of the top
    //            <num> photos with the most tagged users
    //        (B) For each photo identified in (A), find the IDs, first names, and last names
    //            of the users therein tagged
    public FakebookArrayList<TaggedPhotoInfo> findPhotosWithMostTags(int num) throws SQLException {
        FakebookArrayList<TaggedPhotoInfo> results = new FakebookArrayList<TaggedPhotoInfo>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                PhotoInfo p = new PhotoInfo(80, 5, "www.photolink.net", "Winterfell S1");
                UserInfo u1 = new UserInfo(3901, "Jon", "Snow");
                UserInfo u2 = new UserInfo(3902, "Arya", "Stark");
                UserInfo u3 = new UserInfo(3903, "Sansa", "Stark");
                TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
                tp.addTaggedUser(u1);
                tp.addTaggedUser(u2);
                tp.addTaggedUser(u3);
                results.add(tp);
            */
            ArrayList<TaggedPhotoInfo> photoResults = new ArrayList<TaggedPhotoInfo>();
            if (num > 0) {
                ResultSet rst = stmt.executeQuery(
                    "SELECT p.Photo_ID, p.Album_ID, p.Photo_Link, a.Album_Name, t.Tag_Subject_ID, u.First_Name, u.Last_Name, ranked.Tag_Count " + 
                    "FROM (" + 
                        "SELECT Tag_Photo_ID, Tag_Count " + 
                        "FROM (" + 
                            "SELECT Tag_Photo_ID, COUNT(*) AS Tag_Count " + 
                            "FROM " + TagsTable + " " + 
                            "GROUP BY Tag_Photo_ID " + 
                            "ORDER BY COUNT(*) DESC, Tag_Photo_ID ASC" + 
                        ") " + 
                        "WHERE ROWNUM <= " + num + 
                    ") ranked " + 
                    "JOIN " + PhotosTable + " p ON p.Photo_ID = ranked.Tag_Photo_ID " + 
                    "JOIN " + AlbumsTable + " a ON a.Album_ID = p.Album_ID " + 
                    "JOIN " + TagsTable + " t ON t.Tag_Photo_ID = p.Photo_ID " + 
                    "JOIN " + UsersTable + " u ON u.User_ID = t.Tag_Subject_ID " + 
                    "ORDER BY ranked.Tag_Count DESC, p.Photo_ID ASC, u.User_ID ASC"
                );
                long currentPhotoID = -1;
                TaggedPhotoInfo currentPhoto = null;
                while(rst.next()) {
                    long photoID = rst.getLong(1);
                    if(photoID != currentPhotoID) {
                        PhotoInfo photo = new PhotoInfo(photoID, rst.getLong(2), rst.getString(3), rst.getString(4));
                        currentPhoto = new TaggedPhotoInfo(photo);
                        photoResults.add(currentPhoto);
                        currentPhotoID = photoID;
                    }
                    currentPhoto.addTaggedUser(new UserInfo(rst.getLong(5), rst.getString(6), rst.getString(7)));
                }
                rst.close();
            } else {
                return results;
            }
            
            for(TaggedPhotoInfo photo : photoResults) {
                results.add(photo);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 5
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, last names, and birth years of each of the two
    //            users in the top <num> pairs of users that meet each of the following
    //            criteria:
    //              (i) same gender
    //              (ii) tagged in at least one common photo
    //              (iii) difference in birth years is no more than <yearDiff>
    //              (iv) not friends
    //        (B) For each pair identified in (A), find the IDs, links, and IDs and names of
    //            the containing album of each photo in which they are tagged together
    public FakebookArrayList<MatchPair> matchMaker(int num, int yearDiff) throws SQLException {
        FakebookArrayList<MatchPair> results = new FakebookArrayList<MatchPair>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(93103, "Romeo", "Montague");
                UserInfo u2 = new UserInfo(93113, "Juliet", "Capulet");
                MatchPair mp = new MatchPair(u1, 1597, u2, 1597);
                PhotoInfo p = new PhotoInfo(167, 309, "www.photolink.net", "Tragedy");
                mp.addSharedPhoto(p);
                results.add(mp);
            */
           if(num > 0) {
                ResultSet rst = stmt.executeQuery(
                    "SELECT ranked.User1_ID, ranked.User2_ID, u1.First_Name, u1.Last_Name, u1.Year_Of_Birth, u2.First_Name, u2.Last_Name, u2.Year_Of_Birth, p.Photo_ID, p.Album_ID, p.Photo_Link, a.Album_Name " +
                    "FROM (" + 
                        "SELECT User1_ID, User2_ID, Shared_Count " +
                        "FROM (" + 
                            "SELECT t1.Tag_Subject_ID AS User1_ID, t2.Tag_Subject_ID AS User2_ID, COUNT(t1.Tag_Photo_ID) AS Shared_Count " +
                            "FROM " + TagsTable + " t1 " +
                            "JOIN " + TagsTable + " t2 ON t1.Tag_Photo_ID = t2.Tag_Photo_ID " + 
                            "AND t1.Tag_Subject_ID < t2.Tag_Subject_ID " +
                            "JOIN " + UsersTable + " u1 ON u1.User_ID = t1.Tag_Subject_ID " +
                            "JOIN " + UsersTable + " u2 ON u2.User_ID = t2.Tag_Subject_ID " +
                            "WHERE u1.Gender = u2.Gender " + 
                            "AND ABS(u1.Year_Of_Birth - u2.Year_Of_Birth) <= " + yearDiff + " " + 
                            "AND NOT EXISTS (" +
                                "SELECT 1 FROM " + FriendsTable + " f " + 
                                "WHERE f.User1_ID = t1.Tag_Subject_ID " + 
                                "AND f.User2_ID = t2.Tag_Subject_ID " + 
                            ") " +
                            "GROUP BY t1.Tag_Subject_ID, t2.Tag_Subject_ID " +
                            "ORDER BY Shared_Count DESC, t1.Tag_Subject_ID ASC, t2.Tag_Subject_ID ASC" + 
                        ") " +
                        "WHERE ROWNUM <= " + num + " " + 
                    ") ranked " +
                    "JOIN " + UsersTable + " u1 ON u1.User_ID = ranked.User1_ID " +
                    "JOIN " + UsersTable + " u2 ON u2.User_ID = ranked.User2_ID " +
                    "JOIN " + TagsTable + " t1 ON t1.Tag_Subject_ID = ranked.User1_ID " +
                    "JOIN " + TagsTable + " t2 ON t2.Tag_Subject_ID = ranked.User2_ID " + 
                    "AND t1.Tag_Photo_ID = t2.Tag_Photo_ID " +
                    "JOIN " + PhotosTable + " p ON p.Photo_ID = t1.Tag_Photo_ID " +
                    "JOIN " + AlbumsTable + " a ON a.Album_ID = p.Album_ID " +
                    "ORDER BY ranked.Shared_Count DESC, ranked.User1_ID ASC, ranked.User2_ID ASC, p.Photo_ID ASC"
                );
                long currentUser = -1;
                long currentUser2 = -1;
                MatchPair currentPair = null;
                while(rst.next()) {
                    long userID = rst.getLong(1);
                    long user2ID = rst.getLong(2);
                    if(userID != currentUser || user2ID != currentUser2) {
                        UserInfo user = new UserInfo(userID, rst.getString(3), rst.getString(4));
                        UserInfo user2 = new UserInfo(user2ID, rst.getString(6), rst.getString(7));
                        currentPair = new MatchPair(user, rst.getInt(5), user2, rst.getInt(8));
                        results.add(currentPair);
                        currentUser = userID;
                        currentUser2 = user2ID;
                    }
                    PhotoInfo photo = new PhotoInfo(rst.getLong(9), rst.getLong(10), rst.getString(11), rst.getString(12));
                    currentPair.addSharedPhoto(photo);
                }
                rst.close();
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 6
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of each of the two users in
    //            the top <num> pairs of users who are not friends but have a lot of
    //            common friends
    //        (B) For each pair identified in (A), find the IDs, first names, and last names
    //            of all the two users' common friends
    public FakebookArrayList<UsersPair> suggestFriends(int num) throws SQLException {
        FakebookArrayList<UsersPair> results = new FakebookArrayList<UsersPair>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(16, "The", "Hacker");
                UserInfo u2 = new UserInfo(80, "Dr.", "Marbles");
                UserInfo u3 = new UserInfo(192, "Digit", "Le Boid");
                UsersPair up = new UsersPair(u1, u2);
                up.addSharedFriend(u3);
                results.add(up);
            */
           if(num > 0) {
                ResultSet rst = stmt.executeQuery(
                    "WITH Directed AS (" +
                        "SELECT User1_ID AS User_ID, User2_ID AS Friend_ID " + 
                        "FROM " + FriendsTable + " " + 
                        "UNION ALL " +
                        "SELECT User2_ID AS User_ID, User1_ID AS Friend_ID " + 
                        "FROM " + FriendsTable + 
                    ") " +
                    "SELECT ranked.User1_ID, ranked.User2_ID, u1.First_Name, u1.Last_Name, u2.First_Name, u2.Last_Name, f.User_ID, f.First_Name, f.Last_Name " +
                    "FROM (" + 
                        "SELECT User1_ID, User2_ID, Mutual_Count " +
                        "FROM (" + 
                            "SELECT d1.User_ID AS User1_ID, d2.User_ID AS User2_ID, COUNT(d1.Friend_ID) AS Mutual_Count " +
                            "FROM Directed d1 " + 
                            "JOIN Directed d2 ON d1.Friend_ID = d2.Friend_ID " + 
                            "AND d1.User_ID < d2.User_ID " +
                            "WHERE NOT EXISTS (" + 
                                "SELECT 1 " + 
                                "FROM " + FriendsTable + " fr " + 
                                "WHERE fr.User1_ID = d1.User_ID " + 
                                "AND fr.User2_ID = d2.User_ID" + 
                            ") " +
                            "GROUP BY d1.User_ID, d2.User_ID " + 
                            "ORDER BY Mutual_Count DESC, d1.User_ID ASC, d2.User_ID ASC" + 
                        ") " + 
                        "WHERE ROWNUM <= " + num + " " + 
                    ") ranked " +
                    "JOIN " + UsersTable + " u1 ON u1.User_ID = ranked.User1_ID " +
                    "JOIN " + UsersTable + " u2 ON u2.User_ID = ranked.User2_ID " +
                    "JOIN Directed d1 ON d1.User_ID = ranked.User1_ID " +
                    "JOIN Directed d2 ON d2.User_ID = ranked.User2_ID " + 
                    "AND d1.Friend_ID = d2.Friend_ID " +
                    "JOIN " + UsersTable + " f ON f.User_ID = d1.Friend_ID " +
                    "ORDER BY ranked.Mutual_Count DESC, ranked.User1_ID ASC, ranked.User2_ID ASC, f.User_ID ASC"
                );
                long currentUser = -1;
                long currentUser2 = -1;
                UsersPair currentPair = null;
                while(rst.next()) {
                    long userID = rst.getLong(1);
                    long user2ID = rst.getLong(2);
                    if(userID != currentUser || user2ID != currentUser2) {
                        UserInfo user =  new UserInfo(userID, rst.getString(3), rst.getString(4));
                        UserInfo user2 = new UserInfo(user2ID, rst.getString(5), rst.getString(6));
                        currentPair = new UsersPair(user, user2);
                        results.add(currentPair);
                        currentUser = userID;
                        currentUser2 = user2ID;
                    }
                    UserInfo friend = new UserInfo(rst.getLong(7), rst.getString(8), rst.getString(9));
                    currentPair.addSharedFriend(friend);
                }
                rst.close();
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 7
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the name of the state or states in which the most events are held
    //        (B) Find the number of events held in the states identified in (A)
    public EventStateInfo findEventStates() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                EventStateInfo info = new EventStateInfo(50);
                info.addState("Kentucky");
                info.addState("Hawaii");
                info.addState("New Hampshire");
                return info;
            */
            ResultSet rst = stmt.executeQuery(
                "SELECT c.State_Name, COUNT(*) AS Event_Count " + 
                "FROM " + EventsTable + " e " +
                "JOIN " + CitiesTable + " c ON e.Event_City_ID = c.City_ID " +
                "GROUP BY c.State_Name " +
                "ORDER BY Event_Count DESC, c.State_Name ASC"
            );
            EventStateInfo info = null;
            long maxCount = -1;
            while(rst.next()) {
                    long count = rst.getLong(2);
                    if(info == null) {
                        maxCount = count;
                        info = new EventStateInfo(maxCount);
                    }
                    if(count != maxCount) {
                        break;
                    }
                    info.addState(rst.getString(1));
                }
                rst.close();
                return info;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new EventStateInfo(-1);
        }
    }

    @Override
    // Query 8
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the ID, first name, and last name of the oldest friend of the user
    //            with User ID <userID>
    //        (B) Find the ID, first name, and last name of the youngest friend of the user
    //            with User ID <userID>
    public AgeInfo findAgeInfo(long userID) throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo old = new UserInfo(12000000, "Galileo", "Galilei");
                UserInfo young = new UserInfo(80000000, "Neil", "deGrasse Tyson");
                return new AgeInfo(old, young);
            */
            ResultSet rst = stmt.executeQuery(
                "SELECT User_ID, First_Name, Last_Name, Old_Rank, Young_Rank " +
                "FROM (" + 
                    "SELECT u.User_ID, u.First_Name, u.Last_Name, " +
                    "ROW_NUMBER() OVER (" + 
                        "ORDER BY u.Year_Of_Birth ASC, u.Month_Of_Birth ASC, u.Day_Of_Birth ASC, u.User_ID DESC" + 
                    ") AS Old_Rank, " +
                    "ROW_NUMBER() OVER (" + 
                        "ORDER BY u.Year_Of_Birth DESC, u.Month_Of_Birth DESC, u.Day_Of_Birth DESC, u.User_ID DESC" + 
                    ") AS Young_Rank " +
                    "FROM " + UsersTable + " u " + 
                    "JOIN " + FriendsTable + " f ON (" + 
                        "f.User1_ID = " + userID + " AND f.User2_ID = u.User_ID" + 
                    ") OR (" + 
                        "f.User2_ID = " + userID + " AND f.User1_ID = u.User_ID" + 
                    ")" + 
                ") " + 
                "WHERE Old_Rank = 1 OR Young_Rank = 1"
            );
            UserInfo oldest = null;
            UserInfo youngest = null;
            while(rst.next()) {
                UserInfo user = new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3));
                if(rst.getInt(4) == 1) {
                    oldest = user;
                }
                if(rst.getInt(5) == 1) {
                    youngest = user;
                }
            }
            rst.close();
            return new AgeInfo(oldest, youngest);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new AgeInfo(new UserInfo(-1, "ERROR", "ERROR"), new UserInfo(-1, "ERROR", "ERROR"));
        }
    }

    @Override
    // Query 9
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find all pairs of users that meet each of the following criteria
    //              (i) same last name
    //              (ii) same hometown
    //              (iii) are friends
    //              (iv) less than 10 birth years apart
    public FakebookArrayList<SiblingInfo> findPotentialSiblings() throws SQLException {
        FakebookArrayList<SiblingInfo> results = new FakebookArrayList<SiblingInfo>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(81023, "Kim", "Kardashian");
                UserInfo u2 = new UserInfo(17231, "Kourtney", "Kardashian");
                SiblingInfo si = new SiblingInfo(u1, u2);
                results.add(si);
            */
            ResultSet rst = stmt.executeQuery(
                "SELECT u1.User_ID, u1.First_Name, u1.Last_Name, u2.User_ID, u2.First_Name, u2.Last_Name " + 
                "FROM " + FriendsTable + " f " + 
                "JOIN " + UsersTable + " u1 ON u1.User_ID = f.User1_ID " +
                "JOIN " + UsersTable + " u2 ON u2.User_ID = f.User2_ID " +
                "JOIN " + HometownCitiesTable + " h1 ON h1.User_ID = u1.User_ID " +
                "JOIN " + HometownCitiesTable + " h2 ON h2.User_ID = u2.User_ID " +
                "WHERE u1.Last_Name = u2.Last_Name " + 
                "AND h1.Hometown_City_ID = h2.Hometown_City_ID " + 
                "AND u1.Year_of_Birth IS NOT NULL " + 
                "AND u2.Year_of_Birth IS NOT NULL " + 
                "AND ABS(u1.Year_of_Birth - u2.Year_of_Birth) < 10 " +
                "ORDER BY u1.User_ID ASC, u2.User_ID ASC"
            );
            while(rst.next()) {
                UserInfo user1 = new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3));
                UserInfo user2 = new UserInfo(rst.getLong(4), rst.getString(5), rst.getString(6));
                results.add(new SiblingInfo(user1, user2));
            }
            rst.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    // Member Variables
    private Connection oracle;
    private final String UsersTable = FakebookOracleConstants.UsersTable;
    private final String CitiesTable = FakebookOracleConstants.CitiesTable;
    private final String FriendsTable = FakebookOracleConstants.FriendsTable;
    private final String CurrentCitiesTable = FakebookOracleConstants.CurrentCitiesTable;
    private final String HometownCitiesTable = FakebookOracleConstants.HometownCitiesTable;
    private final String ProgramsTable = FakebookOracleConstants.ProgramsTable;
    private final String EducationTable = FakebookOracleConstants.EducationTable;
    private final String EventsTable = FakebookOracleConstants.EventsTable;
    private final String AlbumsTable = FakebookOracleConstants.AlbumsTable;
    private final String PhotosTable = FakebookOracleConstants.PhotosTable;
    private final String TagsTable = FakebookOracleConstants.TagsTable;
}
