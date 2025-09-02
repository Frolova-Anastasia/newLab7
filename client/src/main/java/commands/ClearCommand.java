package commands;

import exceptions.EndInputException;
import exceptions.WrongNumberOfArgsException;
import requests.Request;
import responses.Response;
import utility.CommandSender;

import java.io.IOException;

public class ClearCommand implements ClientCommand{
    private final CommandSender sender;
    private final String username;
    private final String passwordHash;

    public ClearCommand(CommandSender sender, String username, String passwordHash) {
        this.sender = sender;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "очищает коллекцию";
    }

    @Override
    public Response execute(String[] args) throws EndInputException, IOException, ClassNotFoundException, WrongNumberOfArgsException {
        NumberArgsChecker.checkArgs(args, 0);
        Request request = new Request(username, passwordHash);
        sender.send("clear", request);
        return sender.receive();
    }
}
