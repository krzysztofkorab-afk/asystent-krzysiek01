import json, os
from datetime import datetime
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI

app = FastAPI(title="Asystent Krzyśka AI")
client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
MODEL = os.environ.get("OPENAI_MODEL", "gpt-5.6-luna")

class InterpretRequest(BaseModel):
    utterance: str
    now: str
    timezone: str

SYSTEM = '''Jesteś polskim parserem poleceń osobistego asystenta. Użytkownik mówi potocznie.
Interpretuj intencję, a przy przypomnieniach rozwiąż wszystkie terminy względem pola now.
Nie zgaduj konkretnej godziny, jeśli użytkownik podał tylko bardzo nieprecyzyjne określenie i sens wymaga doprecyzowania.
Jeśli poda kilka godzin dla tej samej rzeczy, zwróć kilka czasów.
Zwracaj WYŁĄCZNIE JSON w formacie:
{"action":"create_reminder|unknown","text":"krótka treść przypomnienia","times":["ISO-8601 z offsetem i strefą"],"reply":"krótkie naturalne potwierdzenie po polsku"}
Przykład: dla „w piątek o 7:55 i 14 przypomnij mi napisać do Bartka o plisach” zwróć dwa terminy.
Treść przypomnienia ma być użyteczna na ekranie powiadomienia, bez słów typu „przypomnij mi”.'''

@app.get('/health')
def health(): return {"ok": True, "model": MODEL}

@app.post('/interpret')
def interpret(req: InterpretRequest):
    prompt = f"TERAZ: {req.now}\nSTREFA: {req.timezone}\nPOLECENIE: {req.utterance}"
    try:
        response = client.responses.create(
            model=MODEL,
            input=[{"role":"system","content":SYSTEM},{"role":"user","content":prompt}],
            text={"format":{"type":"json_schema","name":"assistant_command","strict":True,"schema":{
                "type":"object","properties":{
                    "action":{"type":"string","enum":["create_reminder","unknown"]},
                    "text":{"type":"string"},
                    "times":{"type":"array","items":{"type":"string"}},
                    "reply":{"type":"string"}},
                "required":["action","text","times","reply"],"additionalProperties":False
            }}}
        )
        data=json.loads(response.output_text)
        return data
    except Exception as e:
        raise HTTPException(status_code=502, detail=str(e))
