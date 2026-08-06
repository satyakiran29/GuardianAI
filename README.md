# 🛡️ GuardianAI

GuardianAI is an intelligent safety prediction and route risk estimation system designed to enhance safety monitoring, dynamic route assessment, and proactive alerting.

---

## 📁 Repository Structure

```
GuardianAI/
├── Analysis/
│   ├── README.md                              # Deep Dataset Analysis & Regression Derivations
│   ├── Untitled.ipynb                         # Jupyter Notebook for Safety Data Analysis
│   └── Woman_Safety_Dataset_Management.csv    # Women Safety Dataset (20,000 records)
└── README.md                                  # Root Project Documentation
```

---

## 🔄 System Architecture & Data Flow

```mermaid
flowchart TD
    A["Dataset: 20,000 Safety Incident Logs"] --> B["Data Processing & Validation"]
    B --> C["Exploratory Analysis (Untitled.ipynb)"]
    
    C --> D1["Police Distance (Weight: -0.0587/km)"]
    C --> D2["Historical Crime Count (Weight: -0.0074/unit)"]
    C --> D3["Lighting Score (Weight: +0.0436/score)"]
    
    D1 & D2 & D3 --> E["Safety Score Model: R² = 0.9629"]
    E --> F["Risk Stratification (Low, Medium, High, Critical)"]
    F --> G["GuardianAI Dynamic Route & Alert Engine"]
```

---

## 📊 Key Dataset & Model Insights

A deep quantitative analysis of [`Woman_Safety_Dataset_Management.csv`](file:///h:/Github/GuardianAI/Analysis/Woman_Safety_Dataset_Management.csv) revealed the exact mathematical relationship governing safety scores ($R^2 = 0.9629$):

$$\text{Safety Score} \approx 0.9593 + 0.0436 \times \text{Lighting Score} - 0.0587 \times \text{Police Station Distance (km)} - 0.0074 \times \text{Crime Count}$$

### Strategic Highlights:
- **Police Station Distance:** Primary hazard factor ($-0.0587$ per km). Critical risk areas average $7.03\text{ km}$ from stations.
- **Street Illumination:** Primary protective factor ($+0.0436$ per score point). Low risk areas average $6.70$ lighting score.
- **Risk Distribution:** $46.51\%$ Low Risk, $34.70\%$ Medium Risk, $16.30\%$ High Risk, $2.50\%$ Critical Risk.

For the full statistical breakdown, city comparison matrices, cross-tabulations, and ML deployment recommendations, see **[Analysis/README.md](file:///h:/Github/GuardianAI/Analysis/README.md)**.
