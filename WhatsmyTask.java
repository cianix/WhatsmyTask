/**
 * Copyright 2021 Luciano Xumerle
 *
 * WhatsmyTask is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * cnxrename is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with cnxrename. If not, see http://www.gnu.org/licenses/.
 */

import java.util.Arrays;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class WhatsmyTask {


    private final static SimpleDateFormat ddmmmyyyy = new SimpleDateFormat("dd MMM yyyy");
    private final static SimpleDateFormat yyyymmdd = new SimpleDateFormat("yyyyMMdd");
    private final static String WMT="WhatsmyTask" ;
    private final static String version="2021.11.02" ;

    public WhatsmyTask () {
    }

    public static void main (String[] args )
    throws java.io.IOException {


        System.out.println("==============================\n### " + WMT + " " + version + " ###\n==============================");

        if ( args.length > 0 && args[0].equals( "-h" ) ) {
            help(WMT);
            return;
        }

        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:whatsmytask.sqlite";
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            // used by shell
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            System.out.println( "----" );
            String cmd="";
            String p1="";
            String p2="";
            if (args.length>0) cmd=args[0];
            if (args.length>1) p1=args[1];
            if (args.length>2) p2=args[2];
            boolean shell=cmd.toUpperCase().equals("-I");

            do {
                if ( cmd.equals("") ) {
                    jobList( conn, false );
                } else if ( cmd.toUpperCase().equals("L") ) {
                    int llist = 5;
                    if ( getLong(p1)>0 ) llist = (int) getLong(p1);
                    lastModify( conn, llist );
                } else if ( cmd.toUpperCase().equals("C") ) {
                    if ( p1.equals("") )
                        jobList( conn, true );
                    else
                        updateJob( conn, p1, "priority=C"  );
                } else if ( cmd.toUpperCase().equals("S") && ! p1.equals("") ) {
                    infoJOB( conn, p1 );
                } else if ( cmd.toUpperCase().equals("I") ) {
                    System.out.println("INSERT A NEW JOB\n================\n");

                    String[] prioList = new String[] { "L", "N", "U" };
                    String[] sino = new String[] { "Y", "N" };
                    String mainpost="";

                    String title = readInput( "==> Insert the title for the NEW (max 40 chars):", new String[] {} );
                    String info= readInput("==> Insert informations (max 1000 chars):", new String[] {} );

                    String deadline = "";
                    String priority = "";
                    if ( p1.equals("") ) {
                        deadline = readInput("==> Insert deadline (eg. 1 jan 2021):", new String[] {} );
                        priority = readInput("==> Priority ( 'U', 'N', 'L' ):", prioList );
                    } else {
                        mainpost = p1;
                        deadline ="01 Jan 1970";
                        priority = "N";
                    }
                    String ok = readInput("==> OK? (y/n)", sino);
                    newJob( conn, title, info, deadline, priority, mainpost );
                } else if ( cmd.toUpperCase().equals("U") && ! p2.equals("") ) {
                    updateJob( conn, p1, p2 );
                } else if ( cmd.toUpperCase().equals("W") && getLong( p1 )>0 ) {
                    lastNdays( conn, getLong( p1 ) );
                } else if ( cmd.toUpperCase().equals("D") && ! p1.equals("") ) {
                    deleteJob( conn, p1 );
                } else if( cmd.equals("exit") ) {
                    shell=false;
                    System.err.println( "BYE!" );
                } else {
                    if ( shell ) help( " >> " );
                    else help( " - " + WMT );
                }

                if ( shell ) {
                    System.out.print( "myTask> " );
                    String[] aa=split( br.readLine().trim(), ' ' );
                    cmd="";
                    p1="";
                    p2="";
                    if (aa.length>0) cmd=aa[0];
                    if (aa.length>1) p1=aa[1];
                    if (aa.length>2) p2=aa[2];
                }
                System.out.println( "----" );
            } while( shell );

            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * READ A STRING FROM STANDARD INPUT
     **/
    private static String readInput( String message, String[] list ) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println( message );
        String in="";
        while(in.equals("")) {
            try {
                in=br.readLine().trim();

                if ( list.length>0  ) {
                    in = in.toUpperCase();
                    if( Arrays.asList( list ).contains( in ) ) return in;
                    in="";
                }
            } catch ( Exception e ) {
                in="";
            }
        }
        return in;
    }


    /**
    * INSERT A NEW JOB
    **/
    private static void newJob( Connection conn, String title, String info, String deadline, String priority, String mainpost ) {
        long time = Instant.now().getEpochSecond();
        long son  = getLong( mainpost );
        if ( son < 0 ) son = time;

        String sql = "INSERT INTO project ( postdate, mainpost, title, info, deadline, priority ) VALUES ( ?, ?, ?, ?, ?, ? );";

        try {
            Date dl=ddmmmyyyy.parse( deadline );
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, time);
            pstmt.setLong(2, son);
            pstmt.setString(3, title);
            pstmt.setString(4, info);
            pstmt.setString(5, yyyymmdd.format(dl));
            pstmt.setString(6, priority);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * RETURNS THE LIST OF OPENED JOB
     **/
    private static void jobList( Connection conn, boolean showClosed ) {

        String sql = "SELECT postdate, title, deadline, priority FROM project WHERE priority<>'C' and postdate=mainpost ORDER BY priority DESC, deadline;";
        if ( showClosed )
            sql = "SELECT postdate, title, deadline, priority FROM project WHERE priority='C' and postdate=mainpost ORDER BY deadline DESC;";

        try {
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            ResultSet rs  = pstmt.executeQuery();

            while (rs.next()) {
                printMainPost( rs.getLong("postdate"), rs.getString("title"), rs.getString( "deadline" ), rs.getString( "priority" ) );
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * PRINT FORMATTED LINE
     **/
    private static void printMainPost( long postdate, String title, String deadline, String priority  ) {
        try {
            Date dl=yyyymmdd.parse( deadline );
            System.out.println( priority + " > " + ddmmmyyyy.format( dl ) + " | " + postdate + " | " + title );
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * RETURNS THE LIST OF MODIFIED PROJECTS IN LAST N DAYS
     **/
    private static void lastNdays( Connection conn, long days ) {
        String sql= "SELECT a.postdate, b.title AS main, a.title, b.deadline, b.priority "+
                    "FROM project a, project b WHERE b.postdate=a.mainpost AND  a.postdate > ? ORDER BY a.mainpost, a.postdate ;";

        try {
            long time = Instant.now().getEpochSecond();
            time = time - 3600 * 24 * days;
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            pstmt.setLong(1, time);
            ResultSet rs  = pstmt.executeQuery();

            while (rs.next()) {
                printMainPost( rs.getLong("postdate"),  rs.getString("main") + ": " + rs.getString("title"), rs.getString( "deadline" ), rs.getString( "priority" ) );
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }


    /**
     * LIST JOB WITH LAST MODIFY ORDER
     **/
    private static void lastModify( Connection conn, int howMany  ) {
        String sql="SELECT  MAX(postdate), mainpost FROM project GROUP BY ( mainpost ) HAVING priority<>'C' ORDER BY MAX(postdate) DESC;";
        String post = "SELECT postdate, title, deadline, priority FROM project WHERE postdate = ? AND priority<>'C'";
        String[] orders = new String[howMany];
        try {
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            ResultSet rs  = pstmt.executeQuery();
            int i = 0;
            while ( rs.next() && i<howMany ) {
                orders[i] = rs.getString("mainpost");
                i++;
            }
            rs.close();
            pstmt.close();
            PreparedStatement ppp  = conn.prepareStatement(post);
            for ( int j=0; j<i; j++ ) {
                ppp.setLong(1, getLong(orders[j]) );
                rs  = ppp.executeQuery();
                while ( rs.next() ) {
                    printMainPost( rs.getLong("postdate"), rs.getString("title"), rs.getString( "deadline" ), rs.getString( "priority" ) );
                }
                rs.close();
            }
            ppp.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * RETURNS THE LIST OF ALL THE POST WITH THE SAME MAINPOST
     **/
    private static void infoJOB ( Connection conn, String postdate ) {

        String sql = "SELECT postdate, mainpost, title, info, deadline, priority FROM project WHERE mainpost=? ORDER BY postdate;";
        long mod=0;


        try {
            PreparedStatement pstmt  = conn.prepareStatement(sql);

            // set the value
            pstmt.setLong(1, getLong(postdate) );
            ResultSet rs  = pstmt.executeQuery();

            // loop through the result set
            while (rs.next()) {

                long pd = rs.getLong("postdate");
                long mainpost=rs.getLong("mainpost");
                String data=epoch2date( pd );
                String title=rs.getString("title");
                String info= rs.getString("info");

                // look for MOD
                int start=info.indexOf("MOD:");
                String num="";
                if ( start>=0  ) {
                    for ( int i = start+4; i<info.length(); i++ ) {
                        if ( Character.isDigit( info.charAt(i) ) ) num+=info.charAt(i);
                    }
                }
                if ( !num.equals("") ) mod += getLong( num );

                Date deadline=yyyymmdd.parse( rs.getString( "deadline" ) );
                String prio=rs.getString( "priority" );

                if ( pd == mainpost ) {
                    System.out.println( prio + " > " + ddmmmyyyy.format( deadline ) + " | " + pd + " (Created: " + data + ")"  );
                    System.out.println( "Title: " + title + "\n" + printInfo( info ) );
                } else {
                    System.out.println( "\n" + pd + " | " + title + " | " + data );
                    System.out.println( printInfo( info ) );
                }
            }
            if ( mod > 0 ) System.out.println( "----\nTOTAL MOD: " + mod/60 + " hours!" );
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * SOME RULES TO FORMAT OUT THE INFO FIELD
     **/
    private static String printInfo( String info ) {
        StringBuilder s=new StringBuilder();
        int row=0;
        for ( int i=0; i<info.length(); i++ ) {
            if (   row == 0 && Character.isWhitespace(info.charAt(i)) )
                continue;
            row++;
            if ( Character.isWhitespace(info.charAt(i)) && row>70 ) {
                s.append( '\n' );
                row=0;
            } else {
                s.append( info.charAt(i) );
            }
        }
        return s.toString();
    }


    /**
     * DELETE A JOB
     **/
    private static void deleteJob( Connection conn, String postdate ) {
        String[] sino = new String[] { "Y", "N" };
        infoJOB( conn, postdate );
        if ( readInput("==> Are you sure to delete this task with the entire history? (y/n)", sino).equals("Y") ) {
            String sql = "DELETE FROM project WHERE mainpost=? ;";
            try {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setLong(1, getLong( postdate ) );
                pstmt.executeUpdate();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }


    /**
    * UPDATE A FIELD
    **/
    private static void updateJob( Connection conn, String postdate, String field ) {
        String[] a=split( field, '=' );

        if ( a[0].equals( "deadline" ) ) {
            try {
                a[1] = yyyymmdd.format( ddmmmyyyy.parse( a[1] ) );
            }  catch (Exception e) {
            }
        }

        System.out.println( "Updating: " + a[0] + " = " + a[1] );

        String sql = "UPDATE project set " + a[0] + " = ? WHERE postdate = ? ";

        if (  a[0].equals( "priority" )  ) {
            sql = "UPDATE project set " + a[0] + " = ? WHERE mainpost = ? ;";
        }

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, a[1]);
            pstmt.setLong(2, getLong(postdate) );
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * PRINT HELP MESSAGE
     **/
    private static void help( String cmd ) {
        System.out.println( "Print this help:");
        System.out.println( cmd + " -h\n");
        System.out.println( "List open projects ordered by priority and deadline:");
        System.out.println( cmd + "\n");
        System.out.println( "List open projects ordered by last modified posts:");
        System.out.println( cmd + " L\n");
        System.out.println( "List closed projects:");
        System.out.println( cmd + " C\n");
        System.out.println( "List post added during the last N days:");
        System.out.println( cmd + " W <days>\n");
        System.out.println( "Insert a new project:");
        System.out.println( cmd + " I\n");
        System.out.println( "Insert another job to the project");
        System.out.println( cmd + " I <jobID>\n");
        System.out.println( "Show a project:");
        System.out.println( cmd + " S <jobID>\n");
        System.out.println( "Update a project:");
        System.out.println( cmd + " U <jobID> <field>=<string>\n");
        System.out.println( "     field is: title, info, deadline, priority ( 'U', 'N', 'L' )\n");
        System.out.println( "Close a project:");
        System.out.println( cmd + " C <jobID>\n");
        System.out.println( "Delete a project:");
        System.out.println( cmd + " D <jobID>\n");
        System.out.println( "Start the shell mode:");
        System.out.println( cmd + " -I\n" );
        System.out.println( "Exit from shell mode:");
        System.out.println( cmd + " exit" );
    }


    final private static long getLong ( String s ) {
        try {
            return Long.parseLong( s.trim() );
        } catch  ( NumberFormatException err  ) {
            return -1;
        }
    }


    final private static String epoch2date ( long unix_seconds ) {
        Date date = new Date(unix_seconds * 1000L);
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return jdf.format(date);
    }


    final private static String join ( String[] in, String sep ) {
        if ( in.length > 1  ) {
            StringBuilder s = new StringBuilder();
            s.append( in[0] );
            for( int i=1; i<in.length; i++ ) {
                s.append( sep );
                s.append( in[i] );
            }
            return s.toString();
        }
        return in[0];
    }


    final private static String[] split( String string, char separator ) {
        int size=1;
        for (int i=0; i<string.length(); i++)
            if (string.charAt(i) == separator)
                size++;

        String[] res=new String[size];
        int index=0;

        StringBuilder s = new StringBuilder();
        for (int i=0; i<string.length(); i++) {
            char t=string.charAt(i);
            if ( t == separator ) {
                res[index] = s.toString();
                index++;
                s = new StringBuilder();
            } else {
                s.append(t);
            }
        }
        res[index]= s.toString();
        return res;
    }

}  // end class
