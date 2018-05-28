from nltk.tokenize import sent_tokenize
import time

start = time.clock()
fp = open("data/gutenberg/combined.txt", encoding="utf-8")
data = fp.read().replace('\n',' ')
output = open("data/gutenberg/combined-token.txt", 'w', encoding="utf-8")
print(str(time.clock() - start) + " seconds elapsed")
lines = sent_tokenize(data)
for line in lines:
    output.write(line + '\n')
print(str(time.clock() - start) + " seconds elapsed")

# import re
# caps = "([A-Z])"
# prefixes = "(Mr|St|Mrs|Ms|Dr)[.]"
# suffixes = "(Inc|Ltd|Jr|Sr|Co)"
# starters = "(Mr|Mrs|Ms|Dr|He\s|She\s|It\s|They\s|Their\s|Our\s|We\s|But\s|However\s|That\s|This\s|Wherever)"
# acronyms = "([A-Z][.][A-Z][.](?:[A-Z][.])?)"
# websites = "[.](com|net|org|io|gov)"

# def split_into_sentences(text):
#     text = " " + text + "  "
#     text = text.replace("\n"," ")
#     text = re.sub(prefixes,"\\1<prd>",text)
#     text = re.sub(websites,"<prd>\\1",text)
#     if "Ph.D" in text: text = text.replace("Ph.D.","Ph<prd>D<prd>")
#     text = re.sub("\s" + caps + "[.] "," \\1<prd> ",text)
#     text = re.sub(acronyms+" "+starters,"\\1<stop> \\2",text)
#     text = re.sub(caps + "[.]" + caps + "[.]" + caps + "[.]","\\1<prd>\\2<prd>\\3<prd>",text)
#     text = re.sub(caps + "[.]" + caps + "[.]","\\1<prd>\\2<prd>",text)
#     text = re.sub(" "+suffixes+"[.] "+starters," \\1<stop> \\2",text)
#     text = re.sub(" "+suffixes+"[.]"," \\1<prd>",text)
#     text = re.sub(" " + caps + "[.]"," \\1<prd>",text)
#     if "”" in text: text = text.replace(".”","”.")
#     if "\"" in text: text = text.replace(".\"","\".")
#     if "!" in text: text = text.replace("!\"","\"!")
#     if "?" in text: text = text.replace("?\"","\"?")
#     text = text.replace(".",".<stop>")
#     text = text.replace("?","?<stop>")
#     text = text.replace("!","!<stop>")
#     text = text.replace("<prd>",".")
#     sentences = text.split("<stop>")
#     sentences = sentences[:-1]
#     sentences = [s.strip() for s in sentences]
#     return sentences