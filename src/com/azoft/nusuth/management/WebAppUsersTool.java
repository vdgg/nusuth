package com.azoft.nusuth.management;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.gui.MD5;
import com.azoft.nusuth.webappsecurity.impl.UsersConfigModificator;

import java.io.*;
import java.util.*;

public class WebAppUsersTool {
    private final static int NOT_A_COMMAND = 0;
    private final static int CONVERT_COMMAND = 1;
    private final static int ADD_USER_COMMAND = 2;
    private final static int DEL_USER_COMMAND = 3;
    private final static int ADD_ROLE_COMMAND = 4;
    private final static int DEL_ROLE_COMMAND = 5;
    private final static int SET_ROLE_COMMAND = 6;
    private final static int CLEAR_ROLE_COMMAND = 7;
    private final static int EXIT_COMMAND = 8;
    private final static int HELP_COMMAND = 9;
    private final static int SET_PASSWORD_COMMAND = 10;
    private final static int SHOW_USER_COMMAND = 11;
    private final static int SHOW_USERS_COMMAND = 12;
    private final static int SHOW_ROLE_COMMAND = 13;
    private final static int SHOW_ROLES_COMMAND = 14;

    private final static String CONVERT_COMMAND_STRING = "-cryptpassword";
    private final static String ADD_USER_COMMAND_STRING = "-adduser";
    private final static String DEL_USER_COMMAND_STRING = "-deluser";
    private final static String ADD_ROLE_COMMAND_STRING = "-addrole";
    private final static String DEL_ROLE_COMMAND_STRING = "-delrole";
    private final static String SET_ROLE_COMMAND_STRING = "-setrole";
    private final static String CLEAR_ROLE_COMMAND_STRING = "-clearrole";
    private final static String HELP_COMMAND_STRING = "-help";
    private final static String SET_PASSWORD_COMMAND_STRING = "-setpassword";
    private final static String SHOW_USER_COMMAND_STRING = "-showUser";
    private final static String SHOW_ROLE_COMMAND_STRING = "-showRole";
    private final static String SHOW_USERS_COMMAND_STRING = "-showUsers";
    private final static String SHOW_ROLES_COMMAND_STRING = "-showRoles";
    private final static String EXIT_COMMAND_STRING = "-";

    private static BufferedReader inStream = new BufferedReader(new InputStreamReader(System.in));

    private CompositeNusuthWebAppElement configNode;
    private int commandNumber = 0;
    private String[] arguments;
    private boolean isConsole;
    private File configFile;
    private UsersConfigModificator modificator;

    public static void main(String args[]) {
        WebAppUsersTool converter;
        try {
            if (args.length < 1) {
                usage();
                String fileName = askQuestion("Enter file name to process (press <Enter> for exit): ");
                if (fileName.equals(""))
                    return;
                else {
                    String[] arguments = new String[1];
                    arguments[0] = fileName;
                    converter = new WebAppUsersTool(arguments);
                }
            } else {
                converter = new WebAppUsersTool(args);
            }
            converter.process();
            converter.save();
            System.out.println("Done.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    private static void usage() {
        System.out.println("Usage:");
        System.out.println(WebAppUsersTool.class.getName() + " <in file> [command [parameter] [parameter] ...] [command [parameter] [parameter] ...] ...");
        System.out.println("If in file is not given then it will be asked from console.");
        System.out.println("If no commands is given then commands will be asked from console.");
        System.out.println("Commnads:");
        System.out.println("  -addUser <userName> <password>");
        System.out.println("      Create user with specified name and password.");
        System.out.println("  -delUser <userName>");
        System.out.println("      Delete specifyed user and remove him from roles.");
        System.out.println("  -addRole <roleName>");
        System.out.println("      Create role with specifyed name.");
        System.out.println("  -delRole <roleName>");
        System.out.println("      Delete role with specifyed name.");
        System.out.println("  -setRole <userName> <roleName>");
        System.out.println("      Add specifyed user to specifyed role.");
        System.out.println("  -clearRole <userName> <roleName>");
        System.out.println("      Remove specifyed user from specifyed role.");
        System.out.println("  -cryptPassword [userName] [userName] ...");
        System.out.println("      Crypt password for selected users.");
        System.out.println("      if no users selected, crypt passwords for all users.");
        System.out.println("      If password for some users already crypted, it's stay unmodifyed.");
        System.out.println("  -setPassword <userName> <password>");
        System.out.println("      Set specifyed password for selected user.");
        System.out.println("  -showUser <userName>");
        System.out.println("      Show roles for selected user.");
        System.out.println("  -showUsers");
        System.out.println("      Show all user names.");
        System.out.println("  -showRole <roleName>");
        System.out.println("      Show users for selected role.");
        System.out.println("  -showRoles");
        System.out.println("      Show all role names.");
        System.out.println("  -help");
        System.out.println("      Print this message.");
    }

    private static void error(String message) {
        System.err.println(message);
    }

    private static void incorrectCommandSyntaxError(String commandName) {
        error("Incorrect syntax for \"" + commandName + "\" command");
        usage();
    }

    private static String askQuestion(String message) {
        System.out.print(message);
        try {
            return inStream.readLine();
        } catch (IOException ioex) {
            error("Couldn't ask question \"" + message + "\", nested: " + ioex.getMessage());
            return EXIT_COMMAND_STRING;
        }
    }

    private static int getCommand(String command) {
        if (command.equalsIgnoreCase(CONVERT_COMMAND_STRING))
            return CONVERT_COMMAND;
        if (command.equalsIgnoreCase(ADD_USER_COMMAND_STRING))
            return ADD_USER_COMMAND;
        if (command.equalsIgnoreCase(DEL_USER_COMMAND_STRING))
            return DEL_USER_COMMAND;
        if (command.equalsIgnoreCase(ADD_ROLE_COMMAND_STRING))
            return ADD_ROLE_COMMAND;
        if (command.equalsIgnoreCase(DEL_ROLE_COMMAND_STRING))
            return DEL_ROLE_COMMAND;
        if (command.equalsIgnoreCase(SET_ROLE_COMMAND_STRING))
            return SET_ROLE_COMMAND;
        if (command.equalsIgnoreCase(CLEAR_ROLE_COMMAND_STRING))
            return CLEAR_ROLE_COMMAND;
        if (command.equalsIgnoreCase(HELP_COMMAND_STRING))
            return HELP_COMMAND;
        if (command.equalsIgnoreCase(SET_PASSWORD_COMMAND_STRING))
            return SET_PASSWORD_COMMAND;

        if (command.equalsIgnoreCase(SHOW_ROLES_COMMAND_STRING))
            return SHOW_ROLES_COMMAND;
        if (command.equalsIgnoreCase(SHOW_ROLE_COMMAND_STRING))
            return SHOW_ROLE_COMMAND;
        if (command.equalsIgnoreCase(SHOW_USERS_COMMAND_STRING))
            return SHOW_USERS_COMMAND;
        if (command.equalsIgnoreCase(SHOW_USER_COMMAND_STRING))
            return SHOW_USER_COMMAND;

        if (command.equalsIgnoreCase(EXIT_COMMAND_STRING) || command.equals(""))
            return EXIT_COMMAND;

        return NOT_A_COMMAND;
    }

    public WebAppUsersTool(String[] args)
            throws IOException, ParserException {
        configFile = new File(args[0]);
        if (!configFile.exists()) {
            System.out.println("input file doesn't exist!");
            throw new FileNotFoundException("input file \"" + configFile.getAbsolutePath() + "\"doesn't exist!");
        }
        NusuthAppConfigFactory.addEntityResolver("web-app-users", new WebAppUsersEntityResolver());
        configNode = NusuthAppConfigFactory.createConfig("web-app-users", new FileInputStream(configFile));
        modificator = new UsersConfigModificator(configNode);
        if (args.length > 1) {
            isConsole = false;
            arguments = args;
            commandNumber = 1;
        } else {
            isConsole = true;
        }
    }

    private void process() {
        for (String commandString = getCommandString(); !commandString.equals(EXIT_COMMAND_STRING); commandString = getCommandString()) {
            switch (getCommand(commandString)) {
                case CONVERT_COMMAND:
                    {
                        String[] params = getParameters("Enter user name for crypt password (press <Enter> if no more users): ");
                        if (params.length > 0)
                            if (!modificator.convertPasswords(params))
                                error("Couldn't convert password");
                            else {
                                String userNames = null;
                                if (userNames == null)
                                    error("Couldn't get user names from config file");
                                else if (!modificator.convertPasswords(modificator.getUserNames()))
                                    error("Couldn't convert password");
                            }
                        break;
                    }

                case ADD_USER_COMMAND:
                    {
                        String userName = getParameter("Enter user name to add: ");
                        String password = getParameter("Enter user password: ");
                        if (getCommand(userName) == EXIT_COMMAND || getCommand(password) == EXIT_COMMAND)
                            incorrectCommandSyntaxError(ADD_USER_COMMAND_STRING);
                        else if (!modificator.addUser(userName, password))
                            error("Couldn't add user \"" + userName + "\"");
                        break;
                    }

                case DEL_USER_COMMAND:
                    {
                        String userName = getParameter("Enter user name to delete: ");
                        if (getCommand(userName) == EXIT_COMMAND)
                            incorrectCommandSyntaxError(DEL_USER_COMMAND_STRING);
                        else if (!modificator.delUser(userName))
                            error("Couldn't delete user \"" + userName + "\": user not found");
                        break;
                    }

                case ADD_ROLE_COMMAND:
                    {
                        String roleName = getParameter("Enter role name to add: ");
                        if (getCommand(roleName) == EXIT_COMMAND)
                            incorrectCommandSyntaxError(ADD_ROLE_COMMAND_STRING);
                        else if (!modificator.addRole(roleName))
                            error("Couldn't add role \"" + roleName + "\"");
                        break;
                    }

                case DEL_ROLE_COMMAND:
                    {
                        String roleName = getParameter("Enter role name to delete: ");
                        if (getCommand(roleName) == EXIT_COMMAND)
                            incorrectCommandSyntaxError(DEL_ROLE_COMMAND_STRING);
                        else if (!modificator.delRole(roleName))
                            error("Couldn't delete role \"" + roleName + "\": role not found");
                        break;
                    }

                case SET_ROLE_COMMAND:
                    {
                        String userName = getParameter("Enter user name to set role: ");
                        String roleName = getParameter("Enter role name: ");
                        if (getCommand(userName) == EXIT_COMMAND || getCommand(roleName) == EXIT_COMMAND)
                            incorrectCommandSyntaxError(SET_ROLE_COMMAND_STRING);
                        else if (!modificator.setRole(userName, roleName))
                            error("Role \"" + roleName + "\" not found");
                        break;
                    }

                case CLEAR_ROLE_COMMAND:
                    {
                        String userName = getParameter("Enter user name to clear role: ");
                        String roleName = getParameter("Enter role name: ");
                        if (getCommand(userName) == EXIT_COMMAND || getCommand(roleName) == EXIT_COMMAND)
                            incorrectCommandSyntaxError(CLEAR_ROLE_COMMAND_STRING);
                        else if (!modificator.clearRole(userName, roleName))
                            error("Couldn't remove user \"" + userName + "\" from role \"" + roleName + "\": role not found");
                        break;
                    }

                case SET_PASSWORD_COMMAND:
                    {
                        String userName = getParameter("Enter user name to set password: ");
                        String password = getParameter("Enter new password: ");
                        if (getCommand(userName) == EXIT_COMMAND || getCommand(password) == EXIT_COMMAND)
                            incorrectCommandSyntaxError(SET_PASSWORD_COMMAND_STRING);
                        else if (!modificator.setPassword(userName, password))
                            error("Couldn't set password for user \"" + userName + "\"");
                        break;
                    }

                case SHOW_ROLES_COMMAND:
                    {
                        String[] roleNames = modificator.getRoleNames();
                        if (roleNames == null)
                            error("Couldn't show roles");
                        else
                            System.out.println("All roles: " + Arrays.asList(roleNames));
                        break;
                    }

                case SHOW_ROLE_COMMAND:
                    {
                        String roleName = getParameter("Enter role name: ");
                        if (getCommand(roleName) != EXIT_COMMAND) {
                            System.out.println("Role \"" + roleName + '"');
                            String[] userNames = modificator.getRoleUsers(roleName);
                            if (userNames != null)
                                System.out.println("  users: " + Arrays.asList(userNames));
                            else
                                error("Role \"" + roleName + "\" not found");
                        }
                        break;
                    }

                case SHOW_USERS_COMMAND:
                    {
                        String[] userNames = modificator.getUserNames();
                        if (userNames == null)
                            error("Couldn't show users");
                        else
                            System.out.println("All users: " + Arrays.asList(userNames));
                        break;
                    }

                case SHOW_USER_COMMAND:
                    {
                        String userName = getParameter("Enter user name: ");
                        if (getCommand(userName) != EXIT_COMMAND) {
                            System.out.println("User \"" + userName + '"');
                            String[] roleNames = modificator.getUserRoles(userName);
                            if (roleNames != null && roleNames.length > 0)
                                System.out.println("  roles: " + Arrays.asList(roleNames));
                            else
                                error("User \"" + userName + "\" not found or have empty roles list");
                        }
                        break;
                    }

                case HELP_COMMAND:
                    {
                        usage();
                        break;
                    }

                default:
                    error("Unknown command \"" + commandString + "\"");
                    break;
            }
        }
    }

    private String getCommandString() {
        if (isConsole)
            return '-' + askQuestion("Enter a command (simply press <Enter> for exit): ");
        else {
            if (commandNumber < arguments.length)
                return arguments[commandNumber++];
            else
                return EXIT_COMMAND_STRING;
        }
    }

    private String getParameter(String message) {
        if (isConsole)
            return askQuestion(message);
        else {
            if (!arguments[commandNumber].startsWith("-"))
                return arguments[commandNumber++];
            else
                return EXIT_COMMAND_STRING;
        }
    }

    private String[] getParameters(String message) {
        Vector result = new Vector();
        for (String param = getParameter(message); getCommand(param) != EXIT_COMMAND; param = getParameter(message))
            result.add(param);
        return (String[]) result.toArray(new String[0]);
    }

    private void save()
            throws IOException {
        FileOutputStream outStream = new FileOutputStream(configFile);
        outStream.write(configNode.compose("web-app-users", "web-app-users.dtd").getBytes());
        outStream.close();
    }
}

