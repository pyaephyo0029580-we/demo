package com.example.demo.controller;

import com.example.demo.entity.Friend;
import com.example.demo.entity.User;
import com.example.demo.repository.FriendRepository;
import com.example.demo.repository.UserRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;


@Component
public class FriendsController {

    @FXML
    private Label myPlayerIdLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private TextField playerIdField;

    @FXML
    private ListView<String> friendsListView;

    @FXML
    private ListView<Friend> requestsListView;


    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    private final SecureRandom random =
            new SecureRandom();


    private User currentUser;
    private User searchedUser;


    public FriendsController(
            UserRepository userRepository,
            FriendRepository friendRepository
    ) {

        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
    }


    // ==========================================
    // INITIALIZE
    // ==========================================

    @FXML
    public void initialize() {

        setupRequestList();
    }


    // ==========================================
    // RECEIVE LOGIN USER
    // ==========================================

    public void setUserEmail(String email) {

        try {

            Optional<User> optionalUser =
                    userRepository.findByEmail(email);


            if (optionalUser.isEmpty()) {

                messageLabel.setText(
                        "Current user not found."
                );

                return;
            }


            currentUser =
                    optionalUser.get();


            // Friend code မရှိသေးရင် generate
            if (currentUser.getFriendCode() == null ||
                    currentUser.getFriendCode().isBlank()) {

                String code =
                        generateUniqueFriendCode();


                currentUser.setFriendCode(code);


                currentUser =
                        userRepository.save(
                                currentUser
                        );
            }


            showMyPlayerId();

            loadRequests();

            loadFriends();


        } catch (Exception e) {

            e.printStackTrace();

            messageLabel.setText(
                    "Cannot load current user."
            );
        }
    }


    // ==========================================
    // GENERATE FRIEND CODE
    // ==========================================

    private String generateUniqueFriendCode() {

        String code;


        do {

            int number =
                    100000 +
                            random.nextInt(900000);


            code =
                    String.valueOf(number);


        } while (
                userRepository.existsByFriendCode(code)
        );


        return code;
    }


    // ==========================================
    // SHOW OWN PLAYER ID
    // ==========================================

    private void showMyPlayerId() {

        if (currentUser == null) {
            return;
        }


        myPlayerIdLabel.setText(
                currentUser.getUsername()
                        + "#"
                        + currentUser.getFriendCode()
        );
    }


    // ==========================================
    // SEARCH BY NAME#CODE
    // ==========================================

    @FXML
    private void searchPlayer() {

        searchedUser = null;


        String input =
                playerIdField
                        .getText()
                        .trim();


        if (input.isEmpty()) {

            messageLabel.setText(
                    "Enter Player ID."
            );return;
        }


        int hashPosition =
                input.lastIndexOf("#");


        if (hashPosition <= 0 ||
                hashPosition == input.length() - 1) {

            messageLabel.setText(
                    "Use Name#Code."
            );

            return;
        }


        String username =
                input.substring(
                        0,
                        hashPosition
                ).trim();


        String code =
                input.substring(
                        hashPosition + 1
                ).trim();


        if (!code.matches("\\d{6}")) {

            messageLabel.setText(
                    "Friend code must be 6 digits."
            );

            return;
        }


        try {

            Optional<User> optionalUser =
                    userRepository
                            .findByUsernameAndFriendCode(
                                    username,
                                    code
                            );


            if (optionalUser.isEmpty()) {

                messageLabel.setText(
                        "Player not found."
                );

                return;
            }


            User user =
                    optionalUser.get();


            if (currentUser != null &&
                    currentUser.getId()
                            .equals(user.getId())) {

                messageLabel.setText(
                        "You cannot add yourself."
                );

                return;
            }


            searchedUser = user;


            messageLabel.setText(
                    "Found: "
                            + user.getUsername()
                            + "#"
                            + user.getFriendCode()
            );


        } catch (Exception e) {

            e.printStackTrace();

            messageLabel.setText(
                    "Search failed."
            );
        }
    }


    // ==========================================
    // SEND FRIEND REQUEST
    // ==========================================

    @FXML
    private void sendFriendRequest() {

        if (currentUser == null) {

            messageLabel.setText(
                    "Current user not loaded."
            );

            return;
        }


        if (searchedUser == null) {

            messageLabel.setText(
                    "Search Player ID first."
            );

            return;
        }


        Long myId =
                currentUser.getId();


        Long otherId =
                searchedUser.getId();


        try {

            // Current -> Other
            Optional<Friend> first =
                    friendRepository
                            .findByRequesterIdAndReceiverId(
                                    myId,
                                    otherId
                            );


            // Other -> Current
            Optional<Friend> second =
                    friendRepository
                            .findByRequesterIdAndReceiverId(
                                    otherId,
                                    myId
                            );


            if (first.isPresent()) {

                Friend existing =
                        first.get();


                if ("PENDING".equalsIgnoreCase(
                        existing.getStatus()
                )) {

                    messageLabel.setText(
                            "Friend request already sent."
                    );

                } else {

                    messageLabel.setText(
                            "Already friends."
                    );
                }

                return;
            }


            if (second.isPresent()) {

                Friend existing =
                        second.get();


                if ("PENDING".equalsIgnoreCase(
                        existing.getStatus()
                )) {messageLabel.setText(
                        "This player already sent you a request."
                );

                } else {

                    messageLabel.setText(
                            "Already friends."
                    );
                }

                return;
            }


            Friend request =
                    new Friend(
                            myId,
                            otherId,
                            "PENDING"
                    );


            friendRepository.save(
                    request
            );


            messageLabel.setText(
                    "Friend request sent to "
                            + searchedUser.getUsername()
                            + "#"
                            + searchedUser.getFriendCode()
            );


            playerIdField.clear();

            searchedUser = null;


        } catch (Exception e) {

            e.printStackTrace();

            messageLabel.setText(
                    "Cannot send friend request."
            );
        }
    }


    // ==========================================
    // LOAD INCOMING REQUESTS
    // ==========================================

    private void loadRequests() {

        if (currentUser == null) {
            return;
        }


        try {

            List<Friend> requests =
                    friendRepository
                            .findByReceiverIdAndStatus(
                                    currentUser.getId(),
                                    "PENDING"
                            );


            requestsListView
                    .getItems()
                    .clear();


            requestsListView
                    .getItems()
                    .addAll(requests);


        } catch (Exception e) {

            e.printStackTrace();

            messageLabel.setText(
                    "Cannot load requests."
            );
        }
    }


    // ==========================================
    // REQUEST LIST UI
    // ==========================================

    private void setupRequestList() {

        requestsListView.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    Friend friend,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        friend,
                                        empty
                                );


                                if (empty ||
                                        friend == null) {

                                    setText(null);
                                    setGraphic(null);

                                    return;
                                }


                                Optional<User> requester =
                                        userRepository
                                                .findById(
                                                        friend.getRequesterId()
                                                );


                                if (requester.isEmpty()) {

                                    setText(
                                            "Unknown Player"
                                    );

                                    setGraphic(null);

                                    return;
                                }


                                User user =
                                        requester.get();


                                Label playerLabel =
                                        new Label(
                                                user.getUsername()
                                                        + "#"
                                                        + user.getFriendCode()
                                        );playerLabel.setStyle(
                                        "-fx-text-fill: white;" +
                                                "-fx-font-size: 15px;" +
                                                "-fx-font-weight: bold;"
                                );


                                Button acceptButton =
                                        new Button(
                                                "ACCEPT"
                                        );


                                acceptButton.setStyle(
                                        "-fx-background-color: #22c55e;" +
                                                "-fx-text-fill: white;" +
                                                "-fx-font-weight: bold;" +
                                                "-fx-background-radius: 8;"
                                );


                                Button rejectButton =
                                        new Button(
                                                "REJECT"
                                        );


                                rejectButton.setStyle(
                                        "-fx-background-color: #ef4444;" +
                                                "-fx-text-fill: white;" +
                                                "-fx-font-weight: bold;" +
                                                "-fx-background-radius: 8;"
                                );


                                acceptButton.setOnAction(
                                        event ->
                                                acceptRequest(friend)
                                );


                                rejectButton.setOnAction(
                                        event ->
                                                rejectRequest(friend)
                                );


                                Region spacer =
                                        new Region();


                                HBox.setHgrow(
                                        spacer,
                                        Priority.ALWAYS
                                );


                                HBox row =
                                        new HBox(
                                                10,
                                                playerLabel,
                                                spacer,
                                                acceptButton,
                                                rejectButton
                                        );


                                row.setStyle(
                                        "-fx-padding: 10;" +
                                                "-fx-background-color: #1f2937;" +
                                                "-fx-background-radius: 10;"
                                );


                                setGraphic(row);
                            }
                        }
        );
    }


    // ==========================================
    // ACCEPT
    // ==========================================

    private void acceptRequest(
            Friend friend
    ) {

        try {

            friend.setStatus(
                    "ACCEPTED"
            );


            friendRepository.save(
                    friend
            );


            messageLabel.setText(
                    "Friend request accepted."
            );


            loadRequests();

            loadFriends();


        } catch (Exception e) {

            e.printStackTrace();

            messageLabel.setText(
                    "Cannot accept request."
            );
        }
    }


    // ==========================================
    // REJECT
    // ==========================================

    private void rejectRequest(
            Friend friend
    ) {

        try {

            friendRepository.delete(
                    friend
            );


            messageLabel.setText(
                    "Friend request rejected."
            );


            loadRequests();} catch (Exception e) {

            e.printStackTrace();

            messageLabel.setText(
                    "Cannot reject request."
            );
        }
    }


    // ==========================================
    // LOAD ACCEPTED FRIENDS
    // ==========================================

    private void loadFriends() {

        if (currentUser == null) {
            return;
        }


        friendsListView
                .getItems()
                .clear();


        try {

            Long myId =
                    currentUser.getId();


            // Request ကို ကိုယ်ပို့ခဲ့တဲ့ friends
            List<Friend> sent =
                    friendRepository
                            .findByRequesterIdAndStatus(
                                    myId,
                                    "ACCEPTED"
                            );


            for (Friend friend : sent) {

                Optional<User> optionalUser =
                        userRepository.findById(
                                friend.getReceiverId()
                        );


                optionalUser.ifPresent(
                        user ->
                                friendsListView
                                        .getItems()
                                        .add(
                                                user.getUsername()
                                                        + "#"
                                                        + user.getFriendCode()
                                        )
                );
            }


            // Request ကို ကိုယ်လက်ခံခဲ့တဲ့ friends
            List<Friend> received =
                    friendRepository
                            .findByReceiverIdAndStatus(
                                    myId,
                                    "ACCEPTED"
                            );


            for (Friend friend : received) {

                Optional<User> optionalUser =
                        userRepository.findById(
                                friend.getRequesterId()
                        );


                optionalUser.ifPresent(
                        user ->
                                friendsListView
                                        .getItems()
                                        .add(
                                                user.getUsername()
                                                        + "#"
                                                        + user.getFriendCode()
                                        )
                );
            }


            if (friendsListView
                    .getItems()
                    .isEmpty()) {

                friendsListView
                        .getItems()
                        .add(
                                "No friends yet."
                        );
            }


        } catch (Exception e) {

            e.printStackTrace();

            messageLabel.setText(
                    "Cannot load friends."
            );
        }
    }


    // ==========================================
    // REFRESH
    // ==========================================

    @FXML
    private void refreshFriends() {

        loadRequests();

        loadFriends();

        messageLabel.setText(
                "Updated."
        );
    }
}