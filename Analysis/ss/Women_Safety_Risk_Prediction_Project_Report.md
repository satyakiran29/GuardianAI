# Women Safety Risk Prediction using Machine Learning

## Objective
Build a classification model that predicts the risk level of a reported incident using the supplied Women Safety dataset.

## Dataset
- 20,000 rows
- 15 original columns
- Target: `risk_level`
- Target classes: Low, Medium, High, Critical
- Missing values in the supplied dataset: 0

## Workflow
Load Dataset → Understand Data → Data Analysis → Visualization → Missing Value Handling → Outlier Analysis → Categorical Encoding → Feature Scaling → Model Training → Evaluation.

## Preprocessing
- `incident_id`: removed because it is an identifier.
- `incident_timestamp`: converted into hour, month, and day-of-week features.
- `risk_level`: target encoded with `LabelEncoder`.
- Categorical features: `OneHotEncoder(handle_unknown="ignore")`.
- Numerical features: median `SimpleImputer` + `StandardScaler`.
- Outliers: investigated using IQR instead of blindly deleting observations.

## Model
Two models are included:
1. Logistic Regression
2. Random Forest Classifier

The main experiment excludes `safety_score` because it is strongly related to the target and may be a derived score. An optional comparison in the notebook shows the effect of including it.

## Files
- `Women_Safety_Risk_Prediction_Project.ipynb` — complete notebook.
- `Woman_Safety_Dataset_Management(5).csv` — supplied dataset.
