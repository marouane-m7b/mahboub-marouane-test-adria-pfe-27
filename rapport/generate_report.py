#!/usr/bin/env python3
"""Generate the LaTeX report from the project README (standard library only)."""
from pathlib import Path
import re
import unicodedata

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent


def escape(text):
    text = text.replace('’', "'").replace('\u202f', ' ').replace('\u00a0', ' ').replace('—', '-').replace('–', '-')
    mapping = {'\\': r'\textbackslash{}', '&': r'\&', '%': r'\%', '$': r'\$', '#': r'\#',
               '_': r'\_', '{': r'\{', '}': r'\}', '~': r'\textasciitilde{}', '^': r'\textasciicircum{}'}
    return ''.join(mapping.get(c, c) for c in text)


def inline(text):
    pattern = r'(`[^`]+`|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\)|https?://[^\s]+)'
    output = []
    for token in re.split(pattern, text):
        if token.startswith('`') and token.endswith('`'):
            output.append(r'\nolinkurl{' + token[1:-1].replace('’', "'") + '}')
        elif token.startswith('**') and token.endswith('**'):
            output.append(r'\textbf{' + escape(token[2:-2]) + '}')
        elif (match := re.fullmatch(r'\[([^\]]+)\]\(([^)]+)\)', token)):
            label, url = match.groups()
            if url.startswith('http'):
                output.append(r'\href{' + escape(url) + '}{' + escape(label) + '}')
            else:
                output.append(escape(label))
        elif token.startswith(('http://', 'https://')):
            url = token.rstrip('.,')
            output.append(r'\url{' + url + '}' + escape(token[len(url):]))
        else:
            output.append(escape(token))
    return ''.join(output)


PREAMBLE = r'''\documentclass[11pt,a4paper]{report}
\usepackage[T1]{fontenc}
\usepackage[utf8]{inputenc}
\usepackage[french]{babel}
\usepackage{lmodern}
\usepackage[margin=2.3cm,headheight=15pt]{geometry}
\usepackage{graphicx,xcolor,float,booktabs,array,listings,fancyhdr,tabularx}
\usepackage[hidelinks]{hyperref}
\usepackage{xurl}
\usepackage{caption}
\definecolor{accent}{RGB}{20,65,100}
\definecolor{codebg}{RGB}{245,247,249}
\pagestyle{fancy}\fancyhf{}
\fancyhead[L]{\small Test ADRIA PFE 2027}
\fancyhead[R]{\small MAHBOUB Marouane}
\fancyfoot[C]{\thepage}
\setlength{\parindent}{0pt}\setlength{\parskip}{6pt}
\setlength{\emergencystretch}{3em}
\setcounter{tocdepth}{1}
\lstset{basicstyle=\ttfamily\footnotesize,backgroundcolor=\color{codebg},
frame=single,rulecolor=\color{accent},breaklines=true,columns=fullflexible,
keepspaces=true,showstringspaces=false,upquote=true}
\captionsetup{font=small,labelfont=bf}
\newcommand{\capture}[2]{\begin{figure}[H]\centering
\includegraphics[width=\linewidth,height=0.64\textheight,keepaspectratio]{#1}
\caption{#2}\end{figure}}
\begin{document}
\hypersetup{pageanchor=false}
\begin{titlepage}\centering
\includegraphics[width=0.52\textwidth,height=2.5cm,keepaspectratio]{figures/enset-logo.png}\par
\vspace{0.5cm}
{\Large Université Hassan II de Casablanca\par}
{\large ENSET Mohammedia\par}
\vspace{1.1cm}
{\Large Épreuve technique de sélection PFE\par}
\vspace{0.7cm}\rule{\linewidth}{1pt}
\vspace{0.6cm}
{\Huge\bfseries\color{accent} Rapport de réalisation\par}
\vspace{0.7cm}
{\LARGE Plateforme bancaire\par à architecture microservices\par}
\vspace{0.5cm}
{\Large Spring Cloud, REST, GraphQL, MCP et Spring AI\par}
\vspace{0.6cm}\rule{\linewidth}{1pt}\par
\vspace{1cm}
\begin{tabular}{ll}
\textbf{Réalisé par :} & MAHBOUB Marouane\\[0.3cm]
\textbf{Projet :} & Test ADRIA PFE 2027\\[0.3cm]
\textbf{Version :} & Rapport intermédiaire
\end{tabular}
\vfill
{\large Année universitaire 2026--2027\par}
{\large 30 octobre 2026\par}
\end{titlepage}
\hypersetup{pageanchor=true}
\tableofcontents\clearpage
\chapter*{Présentation du projet}
\addcontentsline{toc}{chapter}{Présentation du projet}
Ce projet porte sur une plateforme bancaire composée de deux microservices métier :
la gestion des comptes et l’enregistrement des transactions. Les opérations sont
exposées par REST, GraphQL et MCP. Des services Spring Cloud assurent la découverte,
le routage et la configuration partagée. Un assistant Spring AI utilise les outils
MCP pour interroger les données bancaires.

Ce rapport présente les développements réalisés et les captures obtenues pendant
les tests. Il reprend le README du dépôt et sera complété au fur et à mesure du TP.
Les diagrammes représentent l’architecture cible ; les composants encore à réaliser
sont distingués dans le dernier chapitre.

\begin{table}[H]\centering\small
\begin{tabularx}{\linewidth}{lX}\toprule
Élément & Choix retenu\\\midrule
Langage & Java 21\\
Frameworks & Spring Boot 4.0.8, Spring Cloud 2025.1.3, Spring AI 2.0.1\\
Persistance & Spring Data JPA et H2 en mémoire, une base par service métier\\
Interfaces & REST avec Swagger UI, GraphQL avec GraphiQL, MCP Streamable HTTP\\
Assistant & Groq, modèle \texttt{openai/gpt-oss-120b}\\
Dépôt & \url{https://github.com/marouane-m7b/mahboub-marouane-test-adria-pfe-27}\\\bottomrule
\end{tabularx}\caption{Environnement du projet.}\end{table}
\clearpage
'''


def convert(markdown):
    lines = markdown.splitlines()
    result = []
    index = 0
    in_list = False

    def close_list():
        nonlocal in_list
        if in_list:
            result.append(r'\end{itemize}')
            in_list = False

    while index < len(lines):
        line = lines[index]
        if line.startswith('# ') or line.startswith('Ce README présente'):
            index += 1
            continue
        if not line.strip():
            close_list()
            result.append('')
            index += 1
            continue
        if line.startswith('```'):
            close_list()
            code = []
            index += 1
            while index < len(lines) and not lines[index].startswith('```'):
                code.append(lines[index])
                index += 1
            # listings handles plain ASCII reliably with this template.
            text = '\n'.join(code).replace('’', "'").replace('—', '-').replace('–', '-')
            text = unicodedata.normalize('NFKD', text).encode('ascii', 'ignore').decode()
            result.extend([r'\begin{lstlisting}', text, r'\end{lstlisting}'])
        elif (image := re.fullmatch(r'!\[([^\]]*)\]\(([^)]+)\)', line)):
            close_list()
            caption, path = image.groups()
            if not (ROOT / path).is_file():
                raise FileNotFoundError(path)
            result.append(r'\capture{../' + path + '}{' + escape(caption) + '}')
        elif line.startswith('|'):
            close_list()
            rows = []
            while index < len(lines) and lines[index].startswith('|'):
                row = [cell.strip() for cell in lines[index].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', cell) for cell in row):
                    rows.append(row)
                index += 1
            count = len(rows[0])
            result.append(r'\begin{table}[H]\centering\small')
            result.append(r'\begin{tabularx}{\linewidth}{' + 'X' * count + r'}\toprule')
            for number, row in enumerate(rows):
                result.append(' & '.join(inline(c) for c in row) + r'\\')
                if number == 0:
                    result.append(r'\midrule')
            result.extend([r'\bottomrule\end{tabularx}', r'\end{table}'])
            continue
        elif line.startswith('## '):
            close_list()
            result.append(r'\chapter{' + inline(line[3:]) + '}')
        elif line.startswith('### '):
            close_list()
            title = re.sub(r'^\d+\.\s*', '', line[4:])
            result.append(r'\section{' + inline(title) + '}')
        elif line.startswith('#### '):
            close_list()
            result.append(r'\subsection{' + inline(line[5:]) + '}')
        elif line.startswith('- '):
            if not in_list:
                result.append(r'\begin{itemize}')
                in_list = True
            result.append(r'\item ' + inline(line[2:]))
        else:
            close_list()
            result.append(inline(line))
        index += 1
    close_list()
    return '\n'.join(result)


if __name__ == '__main__':
    source = (ROOT / 'README.md').read_text()
    output = (PREAMBLE + convert(source) + '\n\\end{document}\n').replace('’', "'")
    (HERE / 'rapport_adria.tex').write_text(output)
    print('LaTeX généré : rapport_adria.tex')
