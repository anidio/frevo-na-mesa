// src/components/MesaCard.jsx

import React from 'react';

// Ícone de relógio que será usado no card
const ClockIcon = () => (
  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
);

const MesaCard = ({ mesa, onClick }) => {
  // Objeto para mapear o status às cores do Tailwind.
  const statusStyles = {
    LIVRE: {
      bg: 'bg-green-100',
      border: 'border-green-300',
      text: 'text-green-800',
      tagBg: 'bg-green-200',
    },
    OCUPADA: {
      bg: 'bg-yellow-100',
      border: 'border-yellow-300',
      text: 'text-yellow-800',
      tagBg: 'bg-yellow-200',
    },
    PAGA: {
      bg: 'bg-gray-200',
      border: 'border-gray-400',
      text: 'text-gray-600',
      tagBg: 'bg-gray-300',
    },
  };

  const config = statusStyles[mesa.status];

  return (
    <div 
      onClick={onClick}
      // O layout é flexível na altura, criando o formato retangular
      className={`p-4 border rounded-lg shadow-md transition-transform transform hover:scale-105 cursor-pointer ${config.bg} ${config.border}`}
    >
      <div className="flex items-center justify-between mb-2">
        <span className="font-bold text-lg text-gray-800">Mesa {mesa.numero}</span>
        <span className={`px-2 py-1 text-xs font-semibold rounded-full ${config.tagBg} ${config.text}`}>
          {mesa.status}
        </span>
      </div>
      
      {/* Lógica para exibir o conteúdo principal do card */}
      {mesa.status === 'LIVRE' ? (
        <div className="text-center mt-4 py-2">
          <span className="text-lg font-semibold text-green-800">Disponível</span>
        </div>
      ) : (
        <div className={`space-y-1 text-sm ${config.text}`}>
          <p className="font-bold text-xl text-gray-800">
            R$ {mesa.valorTotal.toFixed(2).replace('.', ',')}
          </p>
          
          {/* >>> AQUI ESTÁ A LÓGICA CORRIGIDA E REINSERIDA <<< */}
          {/* Mostra a hora de abertura apenas se a mesa estiver OCUPADA e tiver a informação */}
          {mesa.status === 'OCUPADA' && mesa.horaAbertura && (
            <div className="flex items-center gap-1 text-xs opacity-80 pt-1 text-gray-600">
              <ClockIcon />
              <span>Aberta às {mesa.horaAbertura}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default MesaCard;