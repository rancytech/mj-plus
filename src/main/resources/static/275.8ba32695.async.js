"use strict";(self.webpackChunkmidjourney_proxy_pool_admin=self.webpackChunkmidjourney_proxy_pool_admin||[]).push([[275],{13275:function(nt,de,v){v.d(de,{Z:function(){return _e}});var s=v(67294),ue=v(76278),fe=v(64894),ge=v(17012),pe=v(62208),me=v(94184),A=v.n(me),ve=v(98423),he=v(53124),q=v(87462),F=v(1413),_=v(91),ee={percent:0,prefixCls:"rc-progress",strokeColor:"#2db7f5",strokeLinecap:"round",strokeWidth:1,trailColor:"#D9D9D9",trailWidth:1,gapPosition:"bottom"},te=function(){var t=(0,s.useRef)([]),r=(0,s.useRef)(null);return(0,s.useEffect)(function(){var o=Date.now(),n=!1;t.current.forEach(function(c){if(c){n=!0;var i=c.style;i.transitionDuration=".3s, .3s, .3s, .06s",r.current&&o-r.current<100&&(i.transitionDuration="0s, 0s")}}),n&&(r.current=Date.now())}),t.current},ye=["className","percent","prefixCls","strokeColor","strokeLinecap","strokeWidth","style","trailColor","trailWidth","transition"],Ce=function(t){var r=(0,F.Z)((0,F.Z)({},ee),t),o=r.className,n=r.percent,c=r.prefixCls,i=r.strokeColor,a=r.strokeLinecap,l=r.strokeWidth,u=r.style,d=r.trailColor,g=r.trailWidth,h=r.transition,y=(0,_.Z)(r,ye);delete y.gapPosition;var C=Array.isArray(n)?n:[n],m=Array.isArray(i)?i:[i],f=te(),$=l/2,S=100-l/2,k="M ".concat(a==="round"?$:0,",").concat($,`
         L `).concat(a==="round"?S:100,",").concat($),p="0 0 100 ".concat(l),E=0;return s.createElement("svg",(0,q.Z)({className:A()("".concat(c,"-line"),o),viewBox:p,preserveAspectRatio:"none",style:u},y),s.createElement("path",{className:"".concat(c,"-line-trail"),d:k,strokeLinecap:a,stroke:d,strokeWidth:g||l,fillOpacity:"0"}),C.map(function(I,P){var x=1;switch(a){case"round":x=1-l/100;break;case"square":x=1-l/2/100;break;default:x=1;break}var D={strokeDasharray:"".concat(I*x,"px, 100px"),strokeDashoffset:"-".concat(E,"px"),transition:h||"stroke-dashoffset 0.3s ease 0s, stroke-dasharray .3s ease 0s, stroke 0.3s linear"},O=m[P]||m[m.length-1];return E+=I,s.createElement("path",{key:P,className:"".concat(c,"-line-path"),d:k,strokeLinecap:a,stroke:O,strokeWidth:l,fillOpacity:"0",ref:function(L){f[P]=L},style:D})}))},Se=Ce,Z=v(71002),ke=v(97685),be=v(98924),re=0,$e=(0,be.Z)();function xe(){var e;return $e?(e=re,re+=1):e="TEST_OR_SSR",e}var Ee=function(e){var t=s.useState(),r=(0,ke.Z)(t,2),o=r[0],n=r[1];return s.useEffect(function(){n("rc_progress_".concat(xe()))},[]),e||o},oe=function(t){var r=t.bg,o=t.children;return s.createElement("div",{style:{width:"100%",height:"100%",background:r}},o)};function ne(e,t){return Object.keys(e).map(function(r){var o=parseFloat(r),n="".concat(Math.floor(o*t),"%");return"".concat(e[r]," ").concat(n)})}var Pe=s.forwardRef(function(e,t){var r=e.prefixCls,o=e.color,n=e.gradientId,c=e.radius,i=e.style,a=e.ptg,l=e.strokeLinecap,u=e.strokeWidth,d=e.size,g=e.gapDegree,h=o&&(0,Z.Z)(o)==="object",y=h?"#FFF":void 0,C=d/2,m=s.createElement("circle",{className:"".concat(r,"-circle-path"),r:c,cx:C,cy:C,stroke:y,strokeLinecap:l,strokeWidth:u,opacity:a===0?0:1,style:i,ref:t});if(!h)return m;var f="".concat(n,"-conic"),$=g?"".concat(180+g/2,"deg"):"0deg",S=ne(o,(360-g)/360),k=ne(o,1),p="conic-gradient(from ".concat($,", ").concat(S.join(", "),")"),E="linear-gradient(to ".concat(g?"bottom":"top",", ").concat(k.join(", "),")");return s.createElement(s.Fragment,null,s.createElement("mask",{id:f},m),s.createElement("foreignObject",{x:0,y:0,width:d,height:d,mask:"url(#".concat(f,")")},s.createElement(oe,{bg:E},s.createElement(oe,{bg:p}))))}),Oe=Pe,R=100,V=function(t,r,o,n,c,i,a,l,u,d){var g=arguments.length>10&&arguments[10]!==void 0?arguments[10]:0,h=o/100*360*((360-i)/360),y=i===0?0:{bottom:0,top:180,left:90,right:-90}[a],C=(100-n)/100*r;u==="round"&&n!==100&&(C+=d/2,C>=r&&(C=r-.01));var m=R/2;return{stroke:typeof l=="string"?l:void 0,strokeDasharray:"".concat(r,"px ").concat(t),strokeDashoffset:C+g,transform:"rotate(".concat(c+h+y,"deg)"),transformOrigin:"".concat(m,"px ").concat(m,"px"),transition:"stroke-dashoffset .3s ease 0s, stroke-dasharray .3s ease 0s, stroke .3s, stroke-width .06s ease .3s, opacity .3s ease 0s",fillOpacity:0}},Le=["id","prefixCls","steps","strokeWidth","trailWidth","gapDegree","gapPosition","trailColor","strokeLinecap","style","className","strokeColor","percent"];function se(e){var t=e!=null?e:[];return Array.isArray(t)?t:[t]}var Ie=function(t){var r=(0,F.Z)((0,F.Z)({},ee),t),o=r.id,n=r.prefixCls,c=r.steps,i=r.strokeWidth,a=r.trailWidth,l=r.gapDegree,u=l===void 0?0:l,d=r.gapPosition,g=r.trailColor,h=r.strokeLinecap,y=r.style,C=r.className,m=r.strokeColor,f=r.percent,$=(0,_.Z)(r,Le),S=R/2,k=Ee(o),p="".concat(k,"-gradient"),E=S-i/2,I=Math.PI*2*E,P=u>0?90+u/2:-90,x=I*((360-u)/360),D=(0,Z.Z)(c)==="object"?c:{count:c,space:2},O=D.count,T=D.space,L=se(f),b=se(m),W=b.find(function(H){return H&&(0,Z.Z)(H)==="object"}),G=W&&(0,Z.Z)(W)==="object",N=G?"butt":h,et=V(I,x,0,100,P,u,d,g,N,i),ce=te(),tt=function(){var K=0;return L.map(function(M,w){var Y=b[w]||b[b.length-1],B=V(I,x,K,M,P,u,d,Y,N,i);return K+=M,s.createElement(Oe,{key:w,color:Y,ptg:M,radius:E,prefixCls:n,gradientId:p,style:B,strokeLinecap:N,strokeWidth:i,gapDegree:u,ref:function(J){ce[w]=J},size:R})}).reverse()},rt=function(){var K=Math.round(O*(L[0]/100)),M=100/O,w=0;return new Array(O).fill(null).map(function(Y,B){var U=B<=K-1?b[0]:g,J=U&&(0,Z.Z)(U)==="object"?"url(#".concat(p,")"):void 0,le=V(I,x,w,M,P,u,d,U,"butt",i,T);return w+=(x-le.strokeDashoffset+T)*100/x,s.createElement("circle",{key:B,className:"".concat(n,"-circle-path"),r:E,cx:S,cy:S,stroke:J,strokeWidth:i,opacity:1,style:le,ref:function(ot){ce[B]=ot}})})};return s.createElement("svg",(0,q.Z)({className:A()("".concat(n,"-circle"),C),viewBox:"0 0 ".concat(R," ").concat(R),style:y,id:o,role:"presentation"},$),!O&&s.createElement("circle",{className:"".concat(n,"-circle-trail"),r:E,cx:S,cy:S,stroke:g,strokeLinecap:N,strokeWidth:a||i,style:et}),O?rt():tt())},ie=Ie,st={Line:Se,Circle:ie},We=v(83062),Q=v(78589);function j(e){return!e||e<0?0:e>100?100:e}function X(e){let{success:t,successPercent:r}=e,o=r;return t&&"progress"in t&&(o=t.progress),t&&"percent"in t&&(o=t.percent),o}const je=e=>{let{percent:t,success:r,successPercent:o}=e;const n=j(X({success:r,successPercent:o}));return[n,j(j(t)-n)]},De=e=>{let{success:t={},strokeColor:r}=e;const{strokeColor:o}=t;return[o||Q.ez.green,r||null]},z=(e,t,r)=>{var o,n,c,i;let a=-1,l=-1;if(t==="step"){const u=r.steps,d=r.strokeWidth;typeof e=="string"||typeof e=="undefined"?(a=e==="small"?2:14,l=d!=null?d:8):typeof e=="number"?[a,l]=[e,e]:[a=14,l=8]=e,a*=u}else if(t==="line"){const u=r==null?void 0:r.strokeWidth;typeof e=="string"||typeof e=="undefined"?l=u||(e==="small"?6:8):typeof e=="number"?[a,l]=[e,e]:[a=-1,l=8]=e}else(t==="circle"||t==="dashboard")&&(typeof e=="string"||typeof e=="undefined"?[a,l]=e==="small"?[60,60]:[120,120]:typeof e=="number"?[a,l]=[e,e]:(a=(n=(o=e[0])!==null&&o!==void 0?o:e[1])!==null&&n!==void 0?n:120,l=(i=(c=e[0])!==null&&c!==void 0?c:e[1])!==null&&i!==void 0?i:120));return[a,l]},Ne=3,we=e=>Ne/e*100;var Ae=e=>{const{prefixCls:t,trailColor:r=null,strokeLinecap:o="round",gapPosition:n,gapDegree:c,width:i=120,type:a,children:l,success:u,size:d=i}=e,[g,h]=z(d,"circle");let{strokeWidth:y}=e;y===void 0&&(y=Math.max(we(g),6));const C={width:g,height:h,fontSize:g*.15+6},m=s.useMemo(()=>{if(c||c===0)return c;if(a==="dashboard")return 75},[c,a]),f=n||a==="dashboard"&&"bottom"||void 0,$=Object.prototype.toString.call(e.strokeColor)==="[object Object]",S=De({success:u,strokeColor:e.strokeColor}),k=A()(`${t}-inner`,{[`${t}-circle-gradient`]:$}),p=s.createElement(ie,{percent:je(e),strokeWidth:y,trailWidth:y,strokeColor:S,strokeLinecap:o,trailColor:r,prefixCls:t,gapDegree:m,gapPosition:f});return s.createElement("div",{className:k,style:C},g<=20?s.createElement(We.Z,{title:l},s.createElement("span",null,p)):s.createElement(s.Fragment,null,p,l))},Ze=function(e,t){var r={};for(var o in e)Object.prototype.hasOwnProperty.call(e,o)&&t.indexOf(o)<0&&(r[o]=e[o]);if(e!=null&&typeof Object.getOwnPropertySymbols=="function")for(var n=0,o=Object.getOwnPropertySymbols(e);n<o.length;n++)t.indexOf(o[n])<0&&Object.prototype.propertyIsEnumerable.call(e,o[n])&&(r[o[n]]=e[o[n]]);return r};const Re=e=>{let t=[];return Object.keys(e).forEach(r=>{const o=parseFloat(r.replace(/%/g,""));isNaN(o)||t.push({key:o,value:e[r]})}),t=t.sort((r,o)=>r.key-o.key),t.map(r=>{let{key:o,value:n}=r;return`${n} ${o}%`}).join(", ")},Te=(e,t)=>{const{from:r=Q.ez.blue,to:o=Q.ez.blue,direction:n=t==="rtl"?"to left":"to right"}=e,c=Ze(e,["from","to","direction"]);if(Object.keys(c).length!==0){const i=Re(c);return{backgroundImage:`linear-gradient(${n}, ${i})`}}return{backgroundImage:`linear-gradient(${n}, ${r}, ${o})`}};var Me=e=>{const{prefixCls:t,direction:r,percent:o,size:n,strokeWidth:c,strokeColor:i,strokeLinecap:a="round",children:l,trailColor:u=null,success:d}=e,g=i&&typeof i!="string"?Te(i,r):{backgroundColor:i},h=a==="square"||a==="butt"?0:void 0,y={backgroundColor:u||void 0,borderRadius:h},C=n!=null?n:[-1,c||(n==="small"?6:8)],[m,f]=z(C,"line",{strokeWidth:c}),$=Object.assign({width:`${j(o)}%`,height:f,borderRadius:h},g),S=X(e),k={width:`${j(S)}%`,height:f,borderRadius:h,backgroundColor:d==null?void 0:d.strokeColor},p={width:m<0?"100%":m,height:f};return s.createElement(s.Fragment,null,s.createElement("div",{className:`${t}-outer`,style:p},s.createElement("div",{className:`${t}-inner`,style:y},s.createElement("div",{className:`${t}-bg`,style:$}),S!==void 0?s.createElement("div",{className:`${t}-success-bg`,style:k}):null)),l)},Be=e=>{const{size:t,steps:r,percent:o=0,strokeWidth:n=8,strokeColor:c,trailColor:i=null,prefixCls:a,children:l}=e,u=Math.round(r*(o/100)),d=t==="small"?2:14,g=t!=null?t:[d,n],[h,y]=z(g,"step",{steps:r,strokeWidth:n}),C=h/r,m=new Array(r);for(let f=0;f<r;f++){const $=Array.isArray(c)?c[f]:c;m[f]=s.createElement("div",{key:f,className:A()(`${a}-steps-item`,{[`${a}-steps-item-active`]:f<=u-1}),style:{backgroundColor:f<=u-1?$:i,width:C,height:y}})}return s.createElement("div",{className:`${a}-steps-outer`},m,l)},Fe=v(77794),Xe=v(14747),ze=v(67968),Ge=v(45503);const ae=e=>{const t=e?"100%":"-100%";return new Fe.E4(`antProgress${e?"RTL":"LTR"}Active`,{"0%":{transform:`translateX(${t}) scaleX(0)`,opacity:.1},"20%":{transform:`translateX(${t}) scaleX(0)`,opacity:.5},to:{transform:"translateX(0) scaleX(1)",opacity:0}})},He=e=>{const{componentCls:t,iconCls:r}=e;return{[t]:Object.assign(Object.assign({},(0,Xe.Wf)(e)),{display:"inline-block","&-rtl":{direction:"rtl"},"&-line":{position:"relative",width:"100%",fontSize:e.fontSize,marginInlineEnd:e.marginXS,marginBottom:e.marginXS},[`${t}-outer`]:{display:"inline-block",width:"100%"},[`&${t}-show-info`]:{[`${t}-outer`]:{marginInlineEnd:`calc(-2em - ${e.marginXS}px)`,paddingInlineEnd:`calc(2em + ${e.paddingXS}px)`}},[`${t}-inner`]:{position:"relative",display:"inline-block",width:"100%",overflow:"hidden",verticalAlign:"middle",backgroundColor:e.remainingColor,borderRadius:e.lineBorderRadius},[`${t}-inner:not(${t}-circle-gradient)`]:{[`${t}-circle-path`]:{stroke:e.defaultColor}},[`${t}-success-bg, ${t}-bg`]:{position:"relative",backgroundColor:e.defaultColor,borderRadius:e.lineBorderRadius,transition:`all ${e.motionDurationSlow} ${e.motionEaseInOutCirc}`},[`${t}-success-bg`]:{position:"absolute",insetBlockStart:0,insetInlineStart:0,backgroundColor:e.colorSuccess},[`${t}-text`]:{display:"inline-block",width:"2em",marginInlineStart:e.marginXS,color:e.colorText,lineHeight:1,whiteSpace:"nowrap",textAlign:"start",verticalAlign:"middle",wordBreak:"normal",[r]:{fontSize:e.fontSize}},[`&${t}-status-active`]:{[`${t}-bg::before`]:{position:"absolute",inset:0,backgroundColor:e.colorBgContainer,borderRadius:e.lineBorderRadius,opacity:0,animationName:ae(),animationDuration:e.progressActiveMotionDuration,animationTimingFunction:e.motionEaseOutQuint,animationIterationCount:"infinite",content:'""'}},[`&${t}-rtl${t}-status-active`]:{[`${t}-bg::before`]:{animationName:ae(!0)}},[`&${t}-status-exception`]:{[`${t}-bg`]:{backgroundColor:e.colorError},[`${t}-text`]:{color:e.colorError}},[`&${t}-status-exception ${t}-inner:not(${t}-circle-gradient)`]:{[`${t}-circle-path`]:{stroke:e.colorError}},[`&${t}-status-success`]:{[`${t}-bg`]:{backgroundColor:e.colorSuccess},[`${t}-text`]:{color:e.colorSuccess}},[`&${t}-status-success ${t}-inner:not(${t}-circle-gradient)`]:{[`${t}-circle-path`]:{stroke:e.colorSuccess}}})}},Ke=e=>{const{componentCls:t,iconCls:r}=e;return{[t]:{[`${t}-circle-trail`]:{stroke:e.remainingColor},[`&${t}-circle ${t}-inner`]:{position:"relative",lineHeight:1,backgroundColor:"transparent"},[`&${t}-circle ${t}-text`]:{position:"absolute",insetBlockStart:"50%",insetInlineStart:0,width:"100%",margin:0,padding:0,color:e.circleTextColor,fontSize:e.circleTextFontSize,lineHeight:1,whiteSpace:"normal",textAlign:"center",transform:"translateY(-50%)",[r]:{fontSize:`${e.fontSize/e.fontSizeSM}em`}},[`${t}-circle&-status-exception`]:{[`${t}-text`]:{color:e.colorError}},[`${t}-circle&-status-success`]:{[`${t}-text`]:{color:e.colorSuccess}}},[`${t}-inline-circle`]:{lineHeight:1,[`${t}-inner`]:{verticalAlign:"bottom"}}}},Ue=e=>{const{componentCls:t}=e;return{[t]:{[`${t}-steps`]:{display:"inline-block","&-outer":{display:"flex",flexDirection:"row",alignItems:"center"},"&-item":{flexShrink:0,minWidth:e.progressStepMinWidth,marginInlineEnd:e.progressStepMarginInlineEnd,backgroundColor:e.remainingColor,transition:`all ${e.motionDurationSlow}`,"&-active":{backgroundColor:e.defaultColor}}}}}},Ve=e=>{const{componentCls:t,iconCls:r}=e;return{[t]:{[`${t}-small&-line, ${t}-small&-line ${t}-text ${r}`]:{fontSize:e.fontSizeSM}}}};var Qe=(0,ze.Z)("Progress",e=>{const t=e.marginXXS/2,r=(0,Ge.TS)(e,{progressStepMarginInlineEnd:t,progressStepMinWidth:t,progressActiveMotionDuration:"2.4s"});return[He(r),Ke(r),Ue(r),Ve(r)]},e=>({circleTextColor:e.colorText,defaultColor:e.colorInfo,remainingColor:e.colorFillSecondary,lineBorderRadius:100,circleTextFontSize:"1em"})),Ye=function(e,t){var r={};for(var o in e)Object.prototype.hasOwnProperty.call(e,o)&&t.indexOf(o)<0&&(r[o]=e[o]);if(e!=null&&typeof Object.getOwnPropertySymbols=="function")for(var n=0,o=Object.getOwnPropertySymbols(e);n<o.length;n++)t.indexOf(o[n])<0&&Object.prototype.propertyIsEnumerable.call(e,o[n])&&(r[o[n]]=e[o[n]]);return r};const lt=null,Je=["normal","exception","active","success"];var qe=s.forwardRef((e,t)=>{const{prefixCls:r,className:o,rootClassName:n,steps:c,strokeColor:i,percent:a=0,size:l="default",showInfo:u=!0,type:d="line",status:g,format:h,style:y}=e,C=Ye(e,["prefixCls","className","rootClassName","steps","strokeColor","percent","size","showInfo","type","status","format","style"]),m=s.useMemo(()=>{var L,b;const W=X(e);return parseInt(W!==void 0?(L=W!=null?W:0)===null||L===void 0?void 0:L.toString():(b=a!=null?a:0)===null||b===void 0?void 0:b.toString(),10)},[a,e.success,e.successPercent]),f=s.useMemo(()=>!Je.includes(g)&&m>=100?"success":g||"normal",[g,m]),{getPrefixCls:$,direction:S,progress:k}=s.useContext(he.E_),p=$("progress",r),[E,I]=Qe(p),P=s.useMemo(()=>{if(!u)return null;const L=X(e);let b;const W=h||(N=>`${N}%`),G=d==="line";return h||f!=="exception"&&f!=="success"?b=W(j(a),j(L)):f==="exception"?b=G?s.createElement(ge.Z,null):s.createElement(pe.Z,null):f==="success"&&(b=G?s.createElement(ue.Z,null):s.createElement(fe.Z,null)),s.createElement("span",{className:`${p}-text`,title:typeof b=="string"?b:void 0},b)},[u,a,m,f,d,p,h]),x=Array.isArray(i)?i[0]:i,D=typeof i=="string"||Array.isArray(i)?i:void 0;let O;d==="line"?O=c?s.createElement(Be,Object.assign({},e,{strokeColor:D,prefixCls:p,steps:c}),P):s.createElement(Me,Object.assign({},e,{strokeColor:x,prefixCls:p,direction:S}),P):(d==="circle"||d==="dashboard")&&(O=s.createElement(Ae,Object.assign({},e,{strokeColor:x,prefixCls:p,progressStatus:f}),P));const T=A()(p,`${p}-status-${f}`,`${p}-${d==="dashboard"&&"circle"||c&&"steps"||d}`,{[`${p}-inline-circle`]:d==="circle"&&z(l,"circle")[0]<=20,[`${p}-show-info`]:u,[`${p}-${l}`]:typeof l=="string",[`${p}-rtl`]:S==="rtl"},k==null?void 0:k.className,o,n,I);return E(s.createElement("div",Object.assign({ref:t,style:Object.assign(Object.assign({},k==null?void 0:k.style),y),className:T,role:"progressbar","aria-valuenow":m},(0,ve.Z)(C,["trailColor","strokeWidth","width","gapDegree","gapPosition","strokeLinecap","success","successPercent"])),O))}),_e=qe}}]);