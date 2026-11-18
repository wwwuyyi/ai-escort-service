// ================================================================
// Mock AI Server - Zero-dependency Node.js HTTP mock
// Ports: 9001 (OCR), 9002 (LLM)
// All medical text is generic reference; NOT medical advice.
// ================================================================

const http = require('http');

// ============ Helper ============
function containsAny(text, keywords) {
  if (!text) return false;
  return keywords.some(k => text.indexOf(k) !== -1);
}

// ============ OCR Mock (port 9001) ============
let ocrCallCount = 0;

const ocrServer = http.createServer((req, res) => {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }
  if (req.method !== 'POST' || !req.url.startsWith('/ocr/')) {
    res.writeHead(404); res.end(JSON.stringify({ error: 'Not Found' })); return;
  }

  let body = '';
  req.on('data', c => body += c);
  req.on('end', () => {
    ocrCallCount++;
    const isHighConf = ocrCallCount % 2 === 1;
    const confidence = isHighConf ? 0.95 : 0.72;
    res.writeHead(200);
    res.end(JSON.stringify({
      result: {
        patientName: 'Zhang**', gender: 'Male', age: 71,
        hospitalName: 'YiNianKang Hospital', department: 'Cardiology',
        examItems: [
          { name: 'WBC', value: '6.8', unit: '10^9/L', referenceRange: '4.0-10.0', abnormal: false },
          { name: 'HGB', value: '132', unit: 'g/L', referenceRange: '120-160', abnormal: false },
          { name: 'FBG', value: '7.2', unit: 'mmol/L', referenceRange: '3.9-6.1', abnormal: true },
          { name: 'TC', value: '5.8', unit: 'mmol/L', referenceRange: '2.8-5.2', abnormal: true }
        ],
        conclusion: 'Blood routine normal; FBG and TC elevated. Recommend endocrinology follow-up.',
        date: '2026-10-28'
      },
      confidence: confidence
    }));
    console.log('[OCR:9001] #' + ocrCallCount + ' conf=' + confidence);
  });
});

// ============ LLM Mock (port 9002) ============
let llmCallCount = 0;

const llmServer = http.createServer((req, res) => {
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }
  if (req.method !== 'POST' || !req.url.startsWith('/llm/')) {
    res.writeHead(404); res.end('<p>Not Found</p>'); return;
  }

  let body = '';
  req.on('data', c => body += c);
  req.on('end', () => {
    llmCallCount++;
    let parsed = {};
    try { parsed = JSON.parse(body) || {}; } catch(e) {}

    // ---- /llm/chat/completions (DeepSeek/OpenAI compatible) ----
    if (req.url === '/llm/chat/completions') {
      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      let messages = (parsed.messages && Array.isArray(parsed.messages)) ? parsed.messages : [];
      let userMsg = '';
      for (let i = messages.length - 1; i >= 0; i--) {
        if (messages[i].role === 'user') { userMsg = messages[i].content || ''; break; }
      }
      if (userMsg.indexOf('TRIGGER_500') !== -1) {
        res.writeHead(500); res.end(JSON.stringify({error:'Internal Server Error'}));
        console.log('[LLM:9002/completions] #' + llmCallCount + ' -> 500');
        return;
      }
      let content = chatReply(userMsg);
      let openaiResp = {
        id: 'chatcmpl-mock-' + llmCallCount,
        object: 'chat.completion',
        created: Math.floor(Date.now() / 1000),
        model: 'deepseek-chat',
        choices: [{ index: 0, message: { role: 'assistant', content: content }, finish_reason: 'stop' }],
        usage: { prompt_tokens: 50, completion_tokens: Math.ceil(content.length / 4), total_tokens: 50 + Math.ceil(content.length / 4) }
      };
      res.writeHead(200); res.end(JSON.stringify(openaiResp));
      console.log('[LLM:9002/completions] #' + llmCallCount + ' -> 200 len=' + content.length);
      return;
    }

    // ---- /llm/chat: auto-detect messages[] vs prompt ----
    if (req.url === '/llm/chat') {
      res.setHeader('Content-Type', 'text/plain; charset=utf-8');

      // New multi-turn conversation: {"messages":[...]}
      if (parsed.messages && Array.isArray(parsed.messages)) {
        let userMsg = '';
        for (let i = parsed.messages.length - 1; i >= 0; i--) {
          if (parsed.messages[i].role === 'user') { userMsg = parsed.messages[i].content || ''; break; }
        }
        if (userMsg.indexOf('TRIGGER_500') !== -1) {
          res.writeHead(500); res.end('Internal Server Error (mock)');
          console.log('[LLM:9002/conv] #' + llmCallCount + ' -> 500');
          return;
        }
        if (userMsg.indexOf('TRIGGER_429') !== -1) {
          res.writeHead(429); res.end('Too Many Requests (mock)');
          console.log('[LLM:9002/conv] #' + llmCallCount + ' -> 429');
          return;
        }
        let resp = chatReply(userMsg);
        res.writeHead(200); res.end(resp);
        console.log('[LLM:9002/conv] #' + llmCallCount + ' -> 200 len=' + resp.length);
        return;
      }

      // Legacy: {"prompt":"..."}
      let prompt = parsed.prompt || '';
      legacyReply(res, prompt);
      return;
    }

    // ---- Other /llm/* paths: legacy ----
    legacyReply(res, parsed.prompt || '');
  });

  function legacyReply(res, prompt) {
    if (prompt.indexOf('TRIGGER_500') !== -1) {
      res.writeHead(500); res.end('Internal Server Error (mock)');
      console.log('[LLM:9002/legacy] #' + llmCallCount + ' -> 500');
      return;
    }
    if (prompt.indexOf('TRIGGER_429') !== -1) {
      res.writeHead(429); res.end('Too Many Requests (mock)');
      console.log('[LLM:9002/legacy] #' + llmCallCount + ' -> 429');
      return;
    }
    let content;
    if (containsAny(prompt, ['NOTICE','notice','gaozhishu','告知书'])) {
      content = noticeTemplate(pickFields(prompt, ['科室','预约时间','行动能力','过敏史']));
    } else if (containsAny(prompt, ['EXPLAIN','explain','jiedu','解读'])) {
      content = explainTemplate(pickFields(prompt, ['诊断','用药建议','备注']));
    } else {
      content = '<p>AI生成内容仅供参考，不构成任何诊疗建议。</p>';
    }
    res.writeHead(200); res.end(content);
    console.log('[LLM:9002/legacy] #' + llmCallCount + ' -> 200 len=' + content.length);
  }
});

// ============ Chat reply: keyword matching (CN+EN) ============
function chatReply(text) {
  var t = text || '';
  var disclaimer = '\n\n---\n*AI生成内容仅供参考，不构成任何诊疗建议。*';

  // Blood sugar / glucose
  if (containsAny(t, ['blood','glucose','sugar','Blood','Glucose','Sugar','xuetang','XUETANG','血糖','糖尿病','空腹'])) {
    return '关于血糖：\n\n' +
      '1. 正常空腹血糖：3.9–6.1 mmol/L\n' +
      '2. 空腹血糖受损（6.1–7.0）：控制饮食、增加运动、定期监测\n' +
      '3. 达到糖尿病范围（≥7.0）：建议到内分泌科就诊\n\n' +
      '日常注意：少吃主食和甜食，饭后散步，定期测血糖。\n' +
      '如出现头晕、乏力、视物模糊等不适，请及时就医。' + disclaimer;
  }

  // Blood pressure
  if (containsAny(t, ['blood pressure','BP','bp','hypertension','Hypertension','xueya','XUEYA','血压','高血压','降压'])) {
    return '关于血压：\n\n' +
      '1. 正常：<120/<80 mmHg\n' +
      '2. 偏高：120–129/<80 mmHg\n' +
      '3. 1级高血压：130–139/80–89 mmHg\n' +
      '4. 2级高血压：≥140/≥90 mmHg\n\n' +
      '日常注意：低盐饮食、规律作息、适度运动，降压药遵医嘱服用。\n' +
      '如出现剧烈头痛、胸闷、视物模糊，请立即就医。' + disclaimer;
  }

  // Medication
  if (containsAny(t, ['medication','medicine','drug','pill','prescription','Medication','Medicine','Drug','Pill','Prescription','chiyao','CHIYAO','fuyao','FUYAO','吃药','用药','药物','服药','药品'])) {
    return '用药注意事项（仅供一般参考，不能替代处方）：\n\n' +
      '1. 按医嘱或说明书服用\n' +
      '2. 不要自行调整剂量或停药\n' +
      '3. 药品存放在阴凉干燥处，部分需要冷藏\n' +
      '4. 就诊时主动告知医生正在服用的所有药物\n' +
      '5. 出现不良反应及时告诉医生\n\n' +
      '慢性病用药需要长期规律服用，不要因为症状好转就自行停药。' + disclaimer;
  }

  // Hospital visit process
  if (containsAny(t, ['appointment','visit','hospital','process','registration','doctor','Appointment','Visit','Hospital','Process','Registration','Doctor','guahao','GUHAO','jiuzhen','JIUZHEN','liucheng','LIUCHENG','kanbing','KANBING','挂号','就诊','看病','流程','就诊流程'])) {
    return '就诊流程：\n\n' +
      '1. 挂号：通过医院APP/网站或现场窗口挂号\n' +
      '2. 签到：提前到院，在自助机或分诊台签到\n' +
      '3. 候诊：在指定区域等待叫号\n' +
      '4. 就诊：向医生说明症状和既往病史\n' +
      '5. 检查/开药：按医生开具的检查单或处方执行\n' +
      '6. 缴费：到窗口或自助机缴费\n' +
      '7. 检查/取药：完成检查或到药房取药\n\n' +
      '请携带：身份证、医保卡、既往病历。抽血检查前需空腹8–12小时，建议提前30分钟到院。' + disclaimer;
  }

  // Pain / symptoms
  if (containsAny(t, ['pain','ache','hurt','discomfort','symptom','Pain','Ache','Hurt','Discomfort','Symptom','teng','TENG','tong','TONG','bushufu','BUSHU','nanshou','NANSHOU','疼','痛','难受','不舒服','症状'])) {
    return '重要提示：AI不能提供医疗诊断。\n\n' +
      '出现以下情况请及时就医：剧烈或持续加重的疼痛、疼痛伴发热/呕吐、受伤后疼痛、疼痛影响日常生活。\n\n' +
      '就诊时请描述：疼痛部位、开始时间和持续时间、疼痛性质（刺痛/胀痛/跳痛）、诱因和缓解因素、其他伴随症状。\n\n' +
      '就诊前：注意休息，不要自行服用止痛药掩盖症状。' + disclaimer;
  }

  // Lab reports / test results
  if (containsAny(t, ['report','test','lab','result','indicator','Report','Test','Lab','Result','Indicator','baogao','BAOGAO','jiancha','JIANCHA','zhibiao','ZHIBIAO','jieguo','JIEGUO','报告','化验','检查','指标','结果'])) {
    return '关于检查报告：\n\n' +
      '1. 参考范围：适用于大多数健康人，轻微超出不一定异常\n' +
      '2. 箭头：↑表示高于参考范围，↓表示低于参考范围\n' +
      '3. 单项异常：需要结合其他指标和症状综合判断\n\n' +
      '常见检查：血常规（贫血/感染）、肝功能（ALT/AST）、肾功能（肌酐/尿素氮）、血脂（胆固醇/甘油三酯）、血糖（糖代谢）\n\n' +
      '具体解读请以医生意见为准。' + disclaimer;
  }

  // Heart / cardiac
  if (containsAny(t, ['heart','cardiac','chest','Heart','Cardiac','Chest','xinzang','XINZANG','xinneike','XINNEIKE','心脏','心内科','胸痛','胸闷','心慌'])) {
    return '心内科相关问题：\n\n' +
      '常见心血管疾病：高血压、冠心病、心律失常、心力衰竭。\n' +
      '常见检查：心电图、心脏超声、运动负荷试验、动态心电图、心肌损伤标志物。\n\n' +
      '就诊准备：携带既往心电图/心脏超声报告，穿宽松衣物，告知医生正在服用的药物。\n' +
      '紧急信号：剧烈胸痛、呼吸困难、心慌伴头晕，请立即到急诊就医。' + disclaimer;
  }

  // Generic fallback
  return '您好，我是陪诊助医AI健康助手，可以帮您解答：\n\n' +
    '- 就诊流程和诊前准备\n' +
    '- 常见检查指标怎么看\n' +
    '- 一般用药注意事项\n' +
    '- 根据症状选择就诊科室\n' +
    '- 日常生活和照护建议\n\n' +
    '请把您的问题说得再详细一些，我会尽量帮您解答。' + disclaimer;
}

// ============ Legacy templates ============
// 从后端中文 prompt 中提取“键=值”字段（值截至下一个英文逗号）
function pickFields(prompt, labels) {
  const fields = {};
  for (const label of labels) {
    const match = prompt.match(new RegExp(label + '=([^,]*)'));
    if (match) fields[label] = match[1].trim();
  }
  return fields;
}

// 将已解析字段按既有模板句式（<p>）原样回填
function fieldLines(fields, labels) {
  return labels
    .filter(label => fields[label] !== undefined && fields[label] !== '')
    .map(label => '<p><strong>' + label + '：</strong>' + fields[label] + '</p>')
    .join('');
}

function noticeTemplate(fields) {
  fields = fields || {};
  return '<h3>诊前就诊告知书</h3>' +
    '<p><strong>亲爱的患者及家属：</strong></p>' +
    '<p>为保证就诊过程顺利，请您阅读以下提示：</p>' +
    fieldLines(fields, ['科室','预约时间','行动能力','过敏史']) +
    '<p>1. <strong>就诊准备：</strong>请携带身份证、医保卡及既往病历资料。</p>' +
    '<p>2. <strong>空腹要求：</strong>抽血检查前需空腹8-12小时（可少量饮水）。</p>' +
    '<p>3. <strong>用药提示：</strong>请勿在未咨询医生的情况下自行停用长期服用的药物。</p>' +
    '<p>4. <strong>陪同提示：</strong>老年人或行动不便者建议有人陪同就诊。</p>' +
    '<p>5. <strong>就诊流程：</strong>挂号 -> 候诊 -> 就诊 -> 检查/处方 -> 缴费 -> 检查/取药。</p>' +
    '<p>6. <strong>出行提示：</strong>建议提前30分钟到达，优先选择公共交通。</p>' +
    '<p>7. <strong>隐私说明：</strong>您的信息仅用于本次就诊。</p>' +
    '<p style="color:#E6A23C;font-weight:bold;">AI生成内容仅供参考，不构成任何诊疗建议</p>';
}

function explainTemplate(fields) {
  fields = fields || {};
  return '<h3>医嘱通俗解读</h3>' +
    '<p><strong>说明：</strong>以下为对医生医嘱的通俗解释。</p>' +
    fieldLines(fields, ['诊断','用药建议','备注']) +
    '<p>1. <strong>诊断：</strong>医生根据您的症状和检查结果作出的专业判断。</p>' +
    '<p>2. <strong>用药：</strong>请按医嘱服药，请勿自行调整用药；如出现副作用请及时告知医生。</p>' +
    '<p>3. <strong>检查：</strong>用于明确诊断或评估治疗效果，请按检查要求做好准备。</p>' +
    '<p>4. <strong>复诊：</strong>请按预约时间复诊，并携带本次就诊的全部检查报告。</p>' +
    '<p>5. <strong>日常照护：</strong>保持均衡饮食、适度运动和充足睡眠，并关注慢性病相关指标。</p>' +
    '<p>6. <strong>紧急情况：</strong>如出现剧烈胸痛、呼吸困难或意识不清，请立即前往急诊就医。</p>' +
    '<p style="color:#E6A23C;font-weight:bold;">AI生成内容仅供参考，不构成任何诊疗建议</p>';
}

// ============ Start ============
ocrServer.listen(9001, () => console.log('[Mock AI] OCR on :9001'));
llmServer.listen(9002, () => console.log('[Mock AI] LLM on :9002'));
console.log('[Mock AI] Ready. /llm/chat supports conversation (messages[]) + legacy (prompt).');