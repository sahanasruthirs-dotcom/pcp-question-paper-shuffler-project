In traditional examination systems, creating multiple versions of MCQ question papers 
manually is time-consuming and increases the risk of malpractice. 
The Automated MCQ Question Paper Shuffler is a Java-based application developed to simplify the 
process of creating multiple versions of MCQ question papers. The main objective of this project is to 
reduce malpractice during examinations by randomizing both the order of questions and the answer 
options for each question. 
In this system, each question is represented using a Question class that stores the question text, four 
options, and the correct answer. The questions are stored using Java’s Collection Framework, and the 
Collections.shuffle() method is used to perform randomization. The shuffling process is done in two 
levels: first, the sequence of questions is shuffled, and then the options within each question are shuffled 
while ensuring that the correct answer remains unchanged. 
The application supports reading questions from external files, which makes the system flexible and 
easy to update. It can generate multiple unique question paper sets such as Set A, Set B, and Set C along 
with their corresponding answer keys. The project is scalable and works efficiently even with a large 
number of questions. Overall, this system provides a practical and efficient solution for generating 
secure and well-organized MCQ examinations.
