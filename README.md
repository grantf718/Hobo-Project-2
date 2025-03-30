# Hobo-Project-2
CSC340 TCP Trivia Networking Game

This is a multi-player trivia game that allows players to connect to a server hosting a trivia game over the network using TCP.
The server reads in 20 questions from 'Questions.txt' and 20 answer groups from 'Answers.txt'. Each answer group contains 4 options, with the first one being the correct answer. The answers are separated by a comma and a space. 

Players get 15 seconds to read the question and poll in. The first player that polls in gets to answer at the end of the 15 seconds. They get 10 seconds to submit an answer. No submission results in a deduction of 20 points. Submitting the correct answer awards 10 points. Submitting an incorrect answer deducts 10 points. 