# 🚀 What Changed

-

---

# 🎯 Why

-

---

# 🧱 Architecture Review Checklist

## 1️⃣ Responsibility
- [ ] Controller가 얇게 유지되었는가?
- [ ] Domain 로직이 Service 계층에 있는가?
- [ ] Repository가 비즈니스 로직을 포함하지 않는가?

## 2️⃣ Query / Index
- [ ] 새로 추가된 쿼리는 인덱스와 정합한가?
- [ ] 정렬/집계는 DB에서 처리되는가?
- [ ] N+1 가능성은 없는가?

## 3️⃣ DTO / API Stability
- [ ] Entity가 직접 노출되지 않았는가?
- [ ] Response shape가 안정적인가?
- [ ] 추후 필드 추가 시 breaking change 가능성은?

## 4️⃣ Transaction Boundary
- [ ] @Transactional 위치가 적절한가?
- [ ] readOnly=true 적용 대상은 분리되었는가?

## 5️⃣ Test Quality
- [ ] "행동"을 검증하는 테스트인가?
- [ ] 경계 조건 테스트가 있는가?
- [ ] 실패 케이스가 포함되었는가?

---

# 📊 Performance Impact

-

---

# 🧠 Future Improvements (Optional)

-