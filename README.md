# Infinite Search

[中文说明](./README_zh.md

## Overview

Based on [Search with Lepton](https://github.com/leptonai/search_with_lepton).
Replaced the backend with SpringBoot + LangChain4J, and made some adjustments to the information processing flow.

Thanks to the great work of [Lepton](https://www.lepton.ai/) for demystifying RAG search, enabling open-source version of [Perplexity](https://www.perplexity.ai/)

## Information Processing Flow

Compared with Search With Lepton, we provide two information processing flows.

### Brief Mode
1. Search keywords with search engine APIs and obtain search results.
2. Aggregate the snippets of the search results as context and integrate it into the prompt.
3. Submit the prompt to the LLM and obtain the results.

### Detail Mode
1. Search keywords with search engine APIs and obtain search results.
2. Store the webpage content of the top two search results and the snippets of the remaining search results in a vector database.
3. Retrieve the most similar texts from the vector database and integrate them into the prompt as contextual information.
4. Submit the prompt to the LLM and obtain the results.

## The keys for the improvement of AI Search

1. How to convert users' search keywords or even language to obtain higher-quality search results
2. How to exclude the interference of other irrelevant content in HTML and extract the most accurate content of the main text
3. How to retrieve more relevant and accurate content from the vector database
4. How to allow the LLM to handle more contextual information

Open browser and visit http://localhost:8605

## Open Documentation
We believe open-source is not just about open source code, but also about open documentation. We are planning a series of documentations to explain the principles of machine learning and RAG development used in this project,

Below is the first article:

[How AI Search Engine Works](https://vlinx.io/blog/how-ai-search-works)

If you are interested in this, please follow us on Twitter to receive the latest information.

https://twitter.com/vlinx_soft

## Build and run this project
[Build](./BUILD.md)