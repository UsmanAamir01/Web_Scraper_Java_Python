import os
import re
import time
import csv
import requests
from bs4 import BeautifulSoup
from concurrent.futures import ThreadPoolExecutor

# Constants
BASE_URL = "https://papers.nips.cc"
SAVE_DIRECTORY = "E:/Python_Scraper/"
CSV_FILE_PATH = os.path.join(SAVE_DIRECTORY, "output.csv")
START_YEAR = 2019
END_YEAR = 2023
THREAD_SLEEP_TIME = 2  # Sleep time in seconds
MAX_THREADS = 10
TIMEOUT = 20  # Timeout for HTTP requests


def create_directory(path):
    """Create a directory if it doesn't exist."""
    os.makedirs(path, exist_ok=True)


def create_csv():
    """Create CSV file and write the header if it doesn't exist."""
    create_directory(SAVE_DIRECTORY)
    if not os.path.exists(CSV_FILE_PATH):
        with open(CSV_FILE_PATH, mode="w", newline="", encoding="utf-8") as file:
            writer = csv.writer(file)
            writer.writerow(["Year", "Title", "Authors", "Abstract", "PDF Link"])


def fetch_page(url):
    """Fetch page content with retry mechanism."""
    for _ in range(3):  # Retry up to 3 times
        try:
            response = requests.get(url, timeout=TIMEOUT)
            if response.status_code == 200:
                return response.text
        except requests.RequestException as e:
            print(f"Error fetching {url}: {e}")
        time.sleep(THREAD_SLEEP_TIME)  # Rate limiting
    return None


def extract_year_from_text(text):
    """Extract year from a given text using regex."""
    match = re.search(r"(\d{4})", text)
    return int(match.group(1)) if match else None


def process_year_page(year_url, year):
    """Extract paper links from a given year's page and process them."""
    html = fetch_page(year_url)
    if not html:
        return

    soup = BeautifulSoup(html, "html.parser")
    paper_links = soup.select("a[href^='/paper/']")  # Fixing paper link selector

    with ThreadPoolExecutor(max_workers=MAX_THREADS) as executor:
        for paper in paper_links:
            paper_url = BASE_URL + paper["href"]
            executor.submit(process_paper_page, paper_url, year)


def process_paper_page(paper_url, year):
    """Extract metadata and PDF link from a paper's page."""
    html = fetch_page(paper_url)
    if not html:
        return

    soup = BeautifulSoup(html, "html.parser")

    title = soup.find("h4").text.strip() if soup.find("h4") else "N/A"
    authors = ", ".join([i.text.strip() for i in soup.find_all("i")])
    abstract_text = soup.find("p").text.strip() if soup.find("p") else "N/A"

    pdf_link_element = soup.select_one("a[href$='.pdf']")
    pdf_url = BASE_URL + pdf_link_element["href"] if pdf_link_element else "N/A"

    save_metadata_to_csv(year, title, authors, abstract_text, pdf_url)

    if pdf_url != "N/A":
        download_pdf(pdf_url, year)


def download_pdf(pdf_url, year):
    """Download and save the PDF."""
    pdf_name = pdf_url.split("/")[-1]
    year_folder = os.path.join(SAVE_DIRECTORY, str(year))
    create_directory(year_folder)
    pdf_path = os.path.join(year_folder, pdf_name)

    try:
        response = requests.get(pdf_url, timeout=TIMEOUT, stream=True)
        with open(pdf_path, "wb") as file:
            for chunk in response.iter_content(chunk_size=8192):
                file.write(chunk)
        print(f"Downloaded: {pdf_name} to {year_folder}")
    except requests.RequestException as e:
        print(f"Error downloading {pdf_name}: {e}")


def save_metadata_to_csv(year, title, authors, abstract_text, pdf_url):
    """Save extracted data to a CSV file."""
    with open(CSV_FILE_PATH, mode="a", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow([year, title, authors, abstract_text, pdf_url])


def main():
    """Main function to scrape research papers from NeurIPS."""
    create_csv()

    homepage_html = fetch_page(BASE_URL)
    if not homepage_html:
        print("Failed to load homepage.")
        return

    soup = BeautifulSoup(homepage_html, "html.parser")
    year_links = soup.select("a[href^='/paper_files/paper/']")

    with ThreadPoolExecutor(max_workers=MAX_THREADS) as executor:
        for link in year_links:
            extracted_year = extract_year_from_text(link.text)
            if extracted_year and START_YEAR <= extracted_year <= END_YEAR:
                year_url = BASE_URL + link["href"]
                executor.submit(process_year_page, year_url, extracted_year)


if __name__ == "__main__":
    main()
