# 📄 **Web Scraping - NeurIPS Papers**  

This project provides **Java and Python scrapers** to extract metadata from **NeurIPS research papers**. The scrapers collect essential details about the papers, such as title, authors, abstract, and publication date, from the NeurIPS website.

---

## 📂 **Submission Contents**  

| File Name                | Description                                                       |
|--------------------------|-------------------------------------------------------------------|
| `scraper.py`              | Python-based web scraper                                          |
| `scraper.java`            | Java-based web scraper                                            |
| `pom.xml`                 | Maven dependencies for Java                                       |
| `README.md`               | Instructions on setup, installation, and usage                    |
| `webscraping_blog.docs`   | Blog post link about the project and comparisons between Python and Java implementations |

---

## 🛠️ **Setup & Requirements**  

### **Python Setup**  

#### **1️⃣ Install Python 3.12.4**  
Ensure that you have Python **3.12.4** installed on your system. If not, you can download it from the official Python website:  
- [Download Python 3.12.4](https://www.python.org/downloads/release/python-3124/)

#### **2️⃣ Install Required Libraries**  
The following Python libraries are required to run the scraper:
- `aiohttp`: For asynchronous HTTP requests.
- `asyncio`: For handling asynchronous tasks.
- `aiofiles`: For asynchronous file handling.
- `beautifulsoup4`: For parsing HTML and extracting data.
- `requests`: For making HTTP requests.

You can install all the required libraries by running the following command in your terminal:


**pip install aiohttp asyncio aiofiles beautifulsoup4 requests**







### **Java Setup & Installation**

#### **1️⃣ Install Java (JDK 17 or later)**
Ensure that you have **JDK 17** or later installed on your system. You can download the JDK from the official Oracle website:  
[Download JDK 17](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)

After installation, confirm that Java is installed correctly by running the following command:

```bash
java -version

**WebScrapingProject**
│
├── scraper.py              # Python script for scraping NeurIPS Papers
├── scraper.java            # Java class for scraping NeurIPS Papers
├── pom.xml                 # Maven file containing Java dependencies
├── README.md               # Documentation with setup and instructions
└── webscraping_blog.docs   # Blog post about the web scraping project
