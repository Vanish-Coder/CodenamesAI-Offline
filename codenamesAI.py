import json
import numpy as np
from sentence_transformers import SentenceTransformer
import random
import math

np.random.seed(None)
random.seed(None)

# -----------------------------
# 1. Load model
# -----------------------------
model = SentenceTransformer("all-MiniLM-L6-v2")

# -----------------------------
# 2. Load game state
# -----------------------------
with open("state.json", "r") as f:
    board = json.load(f)

current_team = board.get("team", "RED")
risk_mode = board.get("risk", "NORMAL").upper()

red_words = [w.upper() for w in board.get("red_words", [])]
blue_words = [w.upper() for w in board.get("blue_words", [])]
neutral_words = [w.upper() for w in board.get("neutral_words", [])]
assassin = board.get("assassin", "").upper()
revealed_words = set(w.upper() for w in board.get("revealed", []))

red_words = [w for w in red_words if w not in revealed_words]
blue_words = [w for w in blue_words if w not in revealed_words]
neutral_words = [w for w in neutral_words if w not in revealed_words]
assassin_unrevealed = assassin not in revealed_words

# -----------------------------
# 3. Team logic (FIXED)
# -----------------------------
if current_team == "BLUE":
    target_words = blue_words
    penalty_words = red_words
else:
    target_words = red_words
    penalty_words = blue_words

if not target_words:
    with open("hint.json", "w") as f:
        json.dump({"clue": "GAME_OVER", "number": 0}, f)
    exit()

# -----------------------------
# 4. Encode vectors
# -----------------------------
target_vecs = model.encode(target_words)
penalty_vecs = model.encode(penalty_words) if penalty_words else []
neutral_vecs = model.encode(neutral_words) if neutral_words else []
assassin_vec = model.encode([assassin])[0] if assassin_unrevealed else None

# -----------------------------
# 5. Candidate clues
# -----------------------------
candidate_clues = [
    "space","planet","star","moon","animal","mammal","bird","fish","fruit","food",
    "vehicle","tool","weapon","machine","computer","phone","music","art","painting",
    "science","nature","forest","ocean","river","mountain","weather","storm","snow",
    "emotion","job","school","sport","game","travel","city","history","myth","legend",
    "energy","force","light","dark","heat","cold","speed","power","sound","shape",
    "object","material","metal","wood","stone","glass"
]

# Remove board words
all_board_words = set(red_words + blue_words + neutral_words + ([assassin] if assassin_unrevealed else []))
candidate_clues = [c for c in candidate_clues if c.upper() not in all_board_words]

candidate_vecs = model.encode(candidate_clues)

# -----------------------------
# 6. Utilities
# -----------------------------
def cosine(a, b):
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

def noun_bias(word):
    # Simple heuristic: abstract verbs/adverbs often end with these
    bad_suffixes = ("ly", "ing", "ed", "ness", "ful", "less")
    return -0.2 if word.endswith(bad_suffixes) else 0.2

# -----------------------------
# 7. Risk parameters
# -----------------------------
RISK = {
    "SAFE":       {"assassin_max": 0.30, "penalty_weight": 1.5, "threshold": 0.85},
    "NORMAL":     {"assassin_max": 0.40, "penalty_weight": 1.0, "threshold": 0.80},
    "AGGRESSIVE": {"assassin_max": 0.55, "penalty_weight": 0.7, "threshold": 0.72}
}[risk_mode]

# -----------------------------
# 8. Scoring function
# -----------------------------
def score_clue(clue, vec):
    # Target reward
    sims = [cosine(vec, tv) for tv in target_vecs]
    target_score = sum(sims)

    # Multi-word bonus
    multi_bonus = sum(1 for s in sims if s > 0.5) * 0.6

    # Penalties
    bad_vecs = list(penalty_vecs) + list(neutral_vecs)
    penalty = np.mean([cosine(vec, bv) for bv in bad_vecs]) if bad_vecs else 0

    # Assassin hard block
    if assassin_vec is not None:
        assassin_sim = cosine(vec, assassin_vec)
        if assassin_sim > RISK["assassin_max"]:
            return -999  # absolute veto
    else:
        assassin_sim = 0

    return (
        target_score
        + multi_bonus
        - penalty * RISK["penalty_weight"]
        + noun_bias(clue)
        - assassin_sim * 2
    )

# -----------------------------
# 9. Rank clues
# -----------------------------
scores = [score_clue(c, v) for c, v in zip(candidate_clues, candidate_vecs)]
sorted_indices = np.argsort(scores)[::-1]

# Pick from top 3 for variety
best_index = random.choice(sorted_indices[:3])
best_clue = candidate_clues[best_index].upper()
best_vec = candidate_vecs[best_index]

# -----------------------------
# 10. Dynamic number selection
# -----------------------------
sims = [cosine(best_vec, tv) for tv in target_vecs]
max_sim = max(sims)
threshold = RISK["threshold"] * max_sim

number = sum(1 for s in sims if s > threshold)
number = max(1, min(number, len(target_words)))

# -----------------------------
# 11. Output
# -----------------------------
hint = {
    "clue": best_clue,
    "number": number
}

with open("hint.json", "w") as f:
    json.dump(hint, f)

print(f"[{current_team} | {risk_mode}] AI Hint: {best_clue} ({number})")
print("Targets:", target_words)
