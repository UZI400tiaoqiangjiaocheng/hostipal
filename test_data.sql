-- ============================================================
-- 智慧医院 测试数据脚本（可重复执行）
-- 用法: mysql --default-character-set=utf8mb4 -uroot -p hospital < test_data.sql
-- ============================================================

-- 1. 清空排班表（避免重复执行时叠加）
DELETE FROM schedule;

-- 2. 患者账号（role=0 表示患者，密码均为 123456）
INSERT INTO user (id, username, password, role, real_name, phone, id_card, status, create_time, update_time) VALUES
(101, 'patient1', '123456', 0, '张三', '13800001111', '110101199001011111', 1, NOW(), NOW()),
(102, 'patient2', '123456', 0, '李四', '13800002222', '110101199202022222', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE id = id;

-- 3. 医生账号（role=1 表示医生）
INSERT INTO user (id, username, password, role, real_name, phone, id_card, status, create_time, update_time) VALUES
(201, 'doc_101', '123456', 1, '王丽', '13900000001', '110101197501011001', 1, NOW(), NOW()),
(202, 'doc_102', '123456', 1, '赵敏', '13900000002', '110101197802021002', 1, NOW(), NOW()),
(203, 'doc_103', '123456', 1, '刘洋', '13900000003', '110101198203031003', 1, NOW(), NOW()),
(204, 'doc_104', '123456', 1, '陈静', '13900000004', '110101198504041004', 1, NOW(), NOW()),
(205, 'doc_105', '123456', 1, '周强', '13900000005', '110101197606051005', 1, NOW(), NOW()),
(206, 'doc_106', '123456', 1, '吴磊', '13900000006', '110101197707061006', 1, NOW(), NOW()),
(207, 'doc_107', '123456', 1, '郑爽', '13900000007', '110101198808081007', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE id = id;

-- 4. 新增科室
INSERT INTO department (id, name, description) VALUES
(101, '妇产科', '妇产科门诊，诊治妇科疾病、孕期保健及产科分娩等'),
(102, '皮肤科', '皮肤科门诊，诊治湿疹、痤疮、皮炎、过敏等各类皮肤病'),
(103, '眼科', '眼科门诊，诊治近视、白内障、青光眼等眼部疾病'),
(104, '耳鼻喉科', '耳鼻喉科门诊，诊治鼻炎、咽喉炎、中耳炎等'),
(105, '骨科', '骨科门诊，诊治骨折、关节炎、颈椎腰椎疾病等'),
(106, '心内科', '心内科门诊，诊治高血压、冠心病、心律失常等心血管疾病')
ON DUPLICATE KEY UPDATE id = id;

-- 5. 新增医生（user_id 与上面医生账号对应，且需唯一）
INSERT INTO doctor (id, user_id, department_id, name, title, introduction, registration_fee) VALUES
(101, 201, 101, '王丽', '主任医师', '妇产科常见病、孕期保健与高危妊娠诊治', 25.00),
(102, 202, 102, '赵敏', '副主任医师', '湿疹、痤疮、皮炎等皮肤疾病诊治', 15.00),
(103, 203, 103, '刘洋', '主治医师', '近视、白内障、青光眼等眼科疾病诊治', 20.00),
(104, 204, 104, '陈静', '副主任医师', '鼻炎、咽喉炎、中耳炎等耳鼻喉疾病诊治', 15.00),
(105, 205, 105, '周强', '主任医师', '骨折、关节炎、颈腰椎疾病诊治', 30.00),
(106, 206, 106, '吴磊', '主任医师', '高血压、冠心病、心律失常等心血管疾病诊治', 35.00),
(107, 207, 3,   '郑爽', '主治医师', '小儿感冒、发热、咳嗽等儿科常见病', 20.00)
ON DUPLICATE KEY UPDATE id = id;

-- 6. 生成排班（今天起 6 天，含现有医生 1/2/3 与新增医生 101-107）
INSERT INTO schedule (doctor_id, department_id, schedule_date, time_slot, total_quota, used_quota)
SELECT d.doctor_id,
       d.department_id,
       dt.d,
       ts.slot,
       IF(ts.slot = '晚上', 10, 20) AS total_quota,
       0 AS used_quota
FROM (
       SELECT 1 AS doctor_id, 1 AS department_id
       UNION ALL SELECT 2, 1
       UNION ALL SELECT 3, 2
       UNION ALL SELECT 101, 101
       UNION ALL SELECT 102, 102
       UNION ALL SELECT 103, 103
       UNION ALL SELECT 104, 104
       UNION ALL SELECT 105, 105
       UNION ALL SELECT 106, 106
       UNION ALL SELECT 107, 3
) d
CROSS JOIN (
       SELECT CURDATE() + INTERVAL 0 DAY AS d UNION ALL
       SELECT CURDATE() + INTERVAL 1 DAY UNION ALL
       SELECT CURDATE() + INTERVAL 2 DAY UNION ALL
       SELECT CURDATE() + INTERVAL 3 DAY UNION ALL
       SELECT CURDATE() + INTERVAL 4 DAY UNION ALL
       SELECT CURDATE() + INTERVAL 5 DAY
) dt
CROSS JOIN (
       SELECT '上午' AS slot UNION ALL
       SELECT '下午' UNION ALL
       SELECT '晚上'
) ts;
